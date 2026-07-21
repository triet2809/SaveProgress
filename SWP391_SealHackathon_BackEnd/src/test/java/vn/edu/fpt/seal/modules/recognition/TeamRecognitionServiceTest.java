package vn.edu.fpt.seal.modules.recognition;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import vn.edu.fpt.seal.common.enums.*;
import vn.edu.fpt.seal.modules.audit.repository.AuditLogRepository;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.recognition.dto.RecognitionDtos;
import vn.edu.fpt.seal.modules.recognition.entity.TeamRecognition;
import vn.edu.fpt.seal.modules.recognition.repository.TeamRecognitionRepository;
import vn.edu.fpt.seal.modules.recognition.service.TeamRecognitionService;
import vn.edu.fpt.seal.modules.resultversion.entity.RoundResultVersion;
import vn.edu.fpt.seal.modules.seeding.entity.EventTeamFinish;
import vn.edu.fpt.seal.modules.seeding.repository.EventTeamFinishRepository;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.teamprofile.entity.TeamProfile;
import vn.edu.fpt.seal.modules.teamprofile.repository.TeamProfileRepository;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.user.entity.User;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.security.CurrentUser;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamRecognitionServiceTest {
    @Mock TeamRecognitionRepository recognitions;
    @Mock EventTeamFinishRepository finishes;
    @Mock TeamProfileRepository profiles;
    @Mock UserRepository users;
    @Mock AuditLogRepository audits;

    TeamRecognitionService service;
    TeamProfile profile;
    User coordinator;

    @BeforeEach
    void setUp() {
        service = new TeamRecognitionService(recognitions, finishes, profiles, users, audits);
        profile = TeamProfile.builder().canonicalName("Renamed team").build();
        profile.setId(UUID.randomUUID());
        coordinator = User.builder().fullName("Coordinator").build();
        coordinator.setId(UUID.randomUUID());
        lenient().when(profiles.findAllById(any())).thenReturn(List.of(profile));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1, 2})
    void fewerThanThreeDistinctCompletedSeasonsDoesNotAward(long count) {
        when(finishes.countDistinctQualifyingEvents(
                profile.getId(), EventStatus.completed, TeamStatus.disqualified)).thenReturn(count);
        when(recognitions.findFirstByTeamProfileIdAndRecognitionCodeOrderByCreatedAtDesc(
                profile.getId(), TeamRecognitionService.VETERAN_CODE)).thenReturn(Optional.empty());

        assertTrue(service.evaluateProfiles(List.of(profile.getId()), coordinator).isEmpty());
        verify(recognitions, never()).saveAndFlush(any());
        verify(audits, never()).save(any());
    }

    @Test
    void threeDistinctImmutableCompletedSeasonsAwardsOnceAndAudits() {
        stubCount(3);
        when(recognitions.findFirstByTeamProfileIdAndRecognitionCodeOrderByCreatedAtDesc(
                profile.getId(), TeamRecognitionService.VETERAN_CODE)).thenReturn(Optional.empty());
        when(recognitions.saveAndFlush(any())).thenAnswer(invocation -> {
            TeamRecognition saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        RecognitionDtos.Summary summary =
                service.evaluateProfiles(List.of(profile.getId()), coordinator).get(profile.getId());

        assertEquals(TeamRecognitionService.VETERAN_CODE, summary.code());
        assertEquals(3, summary.qualificationCount());
        assertTrue(summary.active());
        verify(audits).save(argThat(log ->
                log.getAction() == AuditAction.TEAM_RECOGNITION_AWARDED));
    }

    @Test
    void fourthSeasonUpdatesCountWithoutChangingOriginalEarnedAt() {
        LocalDateTime earnedAt = LocalDateTime.of(2025, 7, 1, 10, 0);
        TeamRecognition existing = activeRecognition(3, earnedAt);
        stubCount(4);
        when(recognitions.findFirstByTeamProfileIdAndRecognitionCodeOrderByCreatedAtDesc(
                profile.getId(), TeamRecognitionService.VETERAN_CODE)).thenReturn(Optional.of(existing));
        when(recognitions.saveAndFlush(existing)).thenReturn(existing);

        RecognitionDtos.Summary summary =
                service.evaluateProfiles(List.of(profile.getId()), coordinator).get(profile.getId());

        assertEquals(4, summary.qualificationCount());
        assertEquals(earnedAt, summary.earnedAt());
        verify(audits, never()).save(any());
    }

    @Test
    void repeatedEvaluationIsIdempotentAndRevokedRecognitionIsNotReactivated() {
        TeamRecognition revoked = activeRecognition(3, LocalDateTime.now().minusYears(1));
        revoked.setActive(false);
        revoked.setRevokedAt(LocalDateTime.now().minusDays(1));
        revoked.setRevokeReason("Administrative correction");
        stubCount(4);
        when(recognitions.findFirstByTeamProfileIdAndRecognitionCodeOrderByCreatedAtDesc(
                profile.getId(), TeamRecognitionService.VETERAN_CODE)).thenReturn(Optional.of(revoked));
        when(recognitions.saveAndFlush(revoked)).thenReturn(revoked);

        assertTrue(service.evaluateProfiles(List.of(profile.getId()), coordinator).isEmpty());
        assertFalse(revoked.isActive());
        assertEquals(4, revoked.getQualificationCount());
        verify(audits, never()).save(any());
    }

    @Test
    void evidenceCountsOneFinishPerEventAndIgnoresCancelledOrDisqualifiedRows() {
        when(profiles.findById(profile.getId())).thenReturn(Optional.of(profile));
        Event completed = event(EventStatus.completed, "Season One");
        Event cancelled = event(EventStatus.cancelled, "Cancelled");
        EventTeamFinish firstTrack = finish(completed, TeamStatus.active, "completed", 2);
        EventTeamFinish duplicateTrack = finish(completed, TeamStatus.active, "completed", 1);
        EventTeamFinish cancelledFinish = finish(cancelled, TeamStatus.active, "completed", 1);
        EventTeamFinish disqualified = finish(event(EventStatus.completed, "Season Two"),
                TeamStatus.disqualified, "disqualified", 3);
        when(finishes.findByTeamProfileIdIn(List.of(profile.getId())))
                .thenReturn(List.of(firstTrack, duplicateTrack, cancelledFinish, disqualified));

        RecognitionDtos.EvidenceResponse evidence = service.evidence(profile.getId());

        assertEquals(1, evidence.distinctQualifyingSeasons());
        assertEquals(1, evidence.qualifyingSeasons().get(0).finalPlacement());
        assertEquals(profile.getId(), evidence.teamProfileId());
    }

    @Test
    void teamNameIsNeverUsedAsRecognitionIdentityAndBatchLookupUsesOneQuery() {
        UUID teamId = UUID.randomUUID();
        TeamRecognition recognition = activeRecognition(5, LocalDateTime.now());
        when(recognitions.findActiveRowsByTeamIds(any())).thenReturn(List.<Object[]>of(
                new Object[]{teamId, recognition}));

        var result = service.activeByTeamIds(List.of(teamId));

        assertEquals(1, result.get(teamId).size());
        verify(recognitions, times(1)).findActiveRowsByTeamIds(any());
        verifyNoInteractions(finishes);
    }

    @Test
    void revokeRetainsRecordAndWritesAudit() {
        TeamRecognition recognition = activeRecognition(3, LocalDateTime.now().minusMonths(2));
        recognition.setId(UUID.randomUUID());
        when(profiles.findById(profile.getId())).thenReturn(Optional.of(profile));
        when(recognitions.findById(recognition.getId())).thenReturn(Optional.of(recognition));
        when(users.findById(coordinator.getId())).thenReturn(Optional.of(coordinator));
        when(finishes.findByTeamProfileIdIn(List.of(profile.getId()))).thenReturn(List.of());
        when(recognitions.findFirstByTeamProfileIdAndRecognitionCodeOrderByCreatedAtDesc(
                profile.getId(), TeamRecognitionService.VETERAN_CODE)).thenReturn(Optional.of(recognition));
        var auth = new UsernamePasswordAuthenticationToken(
                CurrentUser.builder().id(coordinator.getId()).roles(List.of("coordinator")).build(), null);

        service.revoke(profile.getId(), recognition.getId(),
                new RecognitionDtos.RevokeRequest("Incorrect imported evidence"), auth);

        assertFalse(recognition.isActive());
        assertNotNull(recognition.getRevokedAt());
        assertEquals(coordinator, recognition.getRevokedBy());
        assertEquals("Incorrect imported evidence", recognition.getRevokeReason());
        verify(recognitions).save(recognition);
        verify(audits).save(argThat(log ->
                log.getAction() == AuditAction.TEAM_RECOGNITION_REVOKED));
    }

    @Test
    void explicitRestoreCreatesNewActiveRecordAndRetainsRevokedHistory() {
        TeamRecognition revoked = activeRecognition(4, LocalDateTime.now().minusMonths(2));
        revoked.setId(UUID.randomUUID());
        revoked.setActive(false);
        revoked.setRevokedAt(LocalDateTime.now().minusDays(1));
        revoked.setRevokedBy(coordinator);
        revoked.setRevokeReason("Administrative correction");
        TeamRecognition restored = activeRecognition(4, LocalDateTime.now());
        restored.setId(UUID.randomUUID());
        when(profiles.findById(profile.getId())).thenReturn(Optional.of(profile));
        when(recognitions.findById(revoked.getId())).thenReturn(Optional.of(revoked));
        when(finishes.countDistinctQualifyingEvents(
                profile.getId(), EventStatus.completed, TeamStatus.disqualified)).thenReturn(4L);
        when(users.findById(coordinator.getId())).thenReturn(Optional.of(coordinator));
        when(recognitions.saveAndFlush(any())).thenReturn(restored);
        when(finishes.findByTeamProfileIdIn(List.of(profile.getId()))).thenReturn(List.of());
        when(recognitions.findFirstByTeamProfileIdAndRecognitionCodeOrderByCreatedAtDesc(
                profile.getId(), TeamRecognitionService.VETERAN_CODE)).thenReturn(Optional.of(restored));
        var auth = new UsernamePasswordAuthenticationToken(
                CurrentUser.builder().id(coordinator.getId()).roles(List.of("coordinator")).build(), null);

        var evidence = service.restore(profile.getId(), revoked.getId(), auth);

        assertTrue(restored.isActive());
        assertFalse(revoked.isActive());
        assertNotNull(revoked.getRevokedAt());
        assertEquals(coordinator, revoked.getRevokedBy());
        assertEquals("Administrative correction", revoked.getRevokeReason());
        assertEquals(restored.getId(), evidence.recognitionId());
        verify(audits).save(argThat(log ->
                log.getAction() == AuditAction.TEAM_RECOGNITION_RESTORED));
    }

    private void stubCount(long count) {
        when(finishes.countDistinctQualifyingEvents(
                profile.getId(), EventStatus.completed, TeamStatus.disqualified)).thenReturn(count);
    }

    private TeamRecognition activeRecognition(int count, LocalDateTime earnedAt) {
        return TeamRecognition.builder().teamProfile(profile)
                .recognitionCode(TeamRecognitionService.VETERAN_CODE)
                .label(TeamRecognitionService.VETERAN_LABEL)
                .qualificationCount(count).earnedAt(earnedAt).active(true).build();
    }

    private Event event(EventStatus status, String title) {
        Event event = Event.builder().title(title).status(status).build();
        event.setId(UUID.randomUUID());
        return event;
    }

    private EventTeamFinish finish(Event event, TeamStatus status, String completionStatus, int rank) {
        Track track = Track.builder().event(event).name("Track").build();
        track.setId(UUID.randomUUID());
        Team team = Team.builder().teamProfile(profile).track(track).name(UUID.randomUUID().toString())
                .status(status).build();
        team.setId(UUID.randomUUID());
        RoundResultVersion version = RoundResultVersion.builder().build();
        version.setId(UUID.randomUUID());
        EventTeamFinish finish = EventTeamFinish.builder().event(event).track(track).team(team)
                .teamProfile(profile).resultVersion(version).completionStatus(completionStatus)
                .finalRank(rank).completedAt(LocalDateTime.now()).build();
        finish.setId(UUID.randomUUID());
        return finish;
    }
}
