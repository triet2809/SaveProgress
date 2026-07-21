package vn.edu.fpt.seal.modules.seeding;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import vn.edu.fpt.seal.common.enums.*;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.appeal.repository.AppealRepository;
import vn.edu.fpt.seal.modules.audit.repository.AuditLogRepository;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.event.repository.EventRepository;
import vn.edu.fpt.seal.modules.resultversion.entity.*;
import vn.edu.fpt.seal.modules.resultversion.repository.*;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;
import vn.edu.fpt.seal.modules.round.service.CompetitionLifecycleService;
import vn.edu.fpt.seal.modules.seeding.dto.SeedingDtos;
import vn.edu.fpt.seal.modules.seeding.entity.*;
import vn.edu.fpt.seal.modules.seeding.repository.*;
import vn.edu.fpt.seal.modules.seeding.service.SeedingService;
import vn.edu.fpt.seal.modules.recognition.service.TeamRecognitionService;
import vn.edu.fpt.seal.modules.team.entity.*;
import vn.edu.fpt.seal.modules.team.repository.*;
import vn.edu.fpt.seal.modules.teamprofile.entity.TeamProfile;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.track.repository.TrackRepository;
import vn.edu.fpt.seal.modules.user.entity.User;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.security.CurrentUser;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeedingServiceTest {
    @Mock EventRepository events;
    @Mock RoundRepository rounds;
    @Mock RoundResultVersionRepository versions;
    @Mock RoundResultVersionEntryRepository versionEntries;
    @Mock AppealRepository appeals;
    @Mock TeamRepository teams;
    @Mock TrackRepository tracks;
    @Mock TeamMemberRepository members;
    @Mock EventTeamFinishRepository finishes;
    @Mock EventSeedAssignmentRepository assignments;
    @Mock UserRepository users;
    @Mock AuditLogRepository audits;
    @Mock CompetitionLifecycleService lifecycle;
    @Mock TeamRecognitionService recognitionService;

    private SeedingService service;
    private Clock clock;
    private User coordinator;
    private UsernamePasswordAuthenticationToken auth;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC);
        service = new SeedingService(events, rounds, versions, versionEntries, appeals, teams, tracks,
                members, finishes, assignments, users, audits, lifecycle, recognitionService, clock);
        coordinator = user("Coordinator");
        auth = new UsernamePasswordAuthenticationToken(
                CurrentUser.builder().id(coordinator.getId()).roles(List.of("coordinator")).build(), null);
    }

    @Test
    void finalizationCreatesImmutableFinishFromPublishedFinalVersion() {
        FinalFixture f = finalFixture();
        List<EventTeamFinish> saved = new ArrayList<>();
        when(users.findById(coordinator.getId())).thenReturn(Optional.of(coordinator));
        when(finishes.findByTeamId(f.team.getId())).thenReturn(Optional.empty());
        when(finishes.save(any())).thenAnswer(invocation -> {
            EventTeamFinish finish = invocation.getArgument(0);
            finish.setId(UUID.randomUUID());
            saved.add(finish);
            return finish;
        });
        when(finishes.findByEventId(f.event.getId())).thenAnswer(invocation -> saved);

        var response = service.finalizeResults(f.event.getId(), auth);

        assertEquals(1, response.createdCount());
        assertEquals(f.version.getId(), response.finishes().get(0).resultVersionId());
        assertEquals(3, response.finishes().get(0).finalRank());
        verify(audits).save(argThat(log -> log.getAction() == AuditAction.FINALIZE_RESULTS));
        verify(recognitionService).evaluateProfiles(
                argThat(ids -> ids.contains(f.team.getTeamProfile().getId())), any());
    }

    @Test
    void duplicateFinalizationIsIdempotent() {
        FinalFixture f = finalFixture();
        EventTeamFinish existing = finish(f, 3, "completed");
        when(users.findById(coordinator.getId())).thenReturn(Optional.of(coordinator));
        when(finishes.findByTeamId(f.team.getId())).thenReturn(Optional.of(existing));
        when(finishes.findByEventId(f.event.getId())).thenReturn(List.of(existing));

        var response = service.finalizeResults(f.event.getId(), auth);

        assertEquals(0, response.createdCount());
        assertEquals(1, response.existingCount());
        verify(finishes, never()).save(any());
    }

    @Test
    void pendingAppealAndOpenWindowBlockFinalization() {
        FinalFixture pending = finalFixture();
        when(appeals.existsByRoundIdAndStatus(pending.round.getId(), "PENDING")).thenReturn(true);
        assertEquals(409, assertThrows(ApiException.class,
                () -> service.finalizeResults(pending.event.getId(), auth)).getStatus().value());

        reset(events, rounds, lifecycle, appeals, versions, versionEntries);
        FinalFixture open = finalFixture();
        open.round.setAppealDeadline(LocalDateTime.now(clock).plusMinutes(1));
        assertEquals(409, assertThrows(ApiException.class,
                () -> service.finalizeResults(open.event.getId(), auth)).getStatus().value());
    }

    @Test
    void supersededVersionAndHierarchyMismatchAreRejected() {
        FinalFixture superseded = finalFixture();
        when(versions.findByRoundIdAndStatus(superseded.round.getId(), "published"))
                .thenReturn(Optional.empty());
        assertEquals(409, assertThrows(ApiException.class,
                () -> service.finalizeResults(superseded.event.getId(), auth)).getStatus().value());

        reset(events, rounds, lifecycle, appeals, versions, versionEntries);
        FinalFixture mismatch = finalFixture();
        Event other = event(EventStatus.ongoing, "Other");
        Track otherTrack = track(other, "Other");
        mismatch.team.setTrack(otherTrack);
        assertEquals(400, assertThrows(ApiException.class,
                () -> service.finalizeResults(mismatch.event.getId(), auth)).getStatus().value());
    }

    @Test
    void cancelledEventNeverCreatesFinish() {
        Event event = event(EventStatus.cancelled, "Cancelled");
        when(events.findById(event.getId())).thenReturn(Optional.of(event));
        lenient().when(users.findById(coordinator.getId())).thenReturn(Optional.of(coordinator));

        assertEquals(400, assertThrows(ApiException.class,
                () -> service.finalizeResults(event.getId(), auth)).getStatus().value());
        verify(finishes, never()).save(any());
    }

    @Test
    void exactHistoricalRosterWithThreeReturningMembersQualifiesDespiteDifferentTeamName() {
        CandidateFixture f = candidateFixture(3, 3);

        var response = service.candidates(f.currentEvent.getId(), null);

        assertEquals(1, response.candidates().size());
        var candidate = response.candidates().get(0);
        assertEquals("Current Identity Name", candidate.teamName());
        assertEquals(3, candidate.supportingFinishes().get(0).returningMemberCount());
        assertEquals("TIER_2", candidate.suggestedSeedTier());
        assertEquals(1, response.summary().unreviewed());
        assertTrue(response.summary().warning().contains("unreviewed"));
    }

    @Test
    void twoReturningMembersAndRankSixDoNotQualify() {
        CandidateFixture continuityFailure = candidateFixture(2, 3);
        assertTrue(service.candidates(continuityFailure.currentEvent.getId(), null).candidates().isEmpty());

        reset(events, teams, finishes, members, assignments);
        CandidateFixture rankFailure = candidateFixture(3, 6);
        assertTrue(service.candidates(rankFailure.currentEvent.getId(), null).candidates().isEmpty());
    }

    @Test
    void disqualifiedCurrentTeamIsExcludedAndForeignTrackIsRejected() {
        Event event = event(EventStatus.ongoing, "Current");
        Track ownTrack = track(event, "Own");
        Team disqualified = team(ownTrack, profile("Profile"), "Disqualified", TeamStatus.disqualified);
        when(events.findById(event.getId())).thenReturn(Optional.of(event));
        when(teams.findByTrackEventId(event.getId())).thenReturn(List.of(disqualified));

        assertTrue(service.candidates(event.getId(), null).candidates().isEmpty());

        Event otherEvent = event(EventStatus.ongoing, "Other");
        Track foreignTrack = track(otherEvent, "Foreign");
        when(tracks.findById(foreignTrack.getId())).thenReturn(Optional.of(foreignTrack));
        assertEquals(400, assertThrows(ApiException.class,
                () -> service.candidates(event.getId(), foreignTrack.getId())).getStatus().value());
    }

    @Test
    void anotherEventsTeamCannotReceiveSeedDecision() {
        Event event = event(EventStatus.ongoing, "Selected");
        Event other = event(EventStatus.ongoing, "Other");
        Team foreignTeam = team(track(other, "Other track"), profile("Foreign"), "Foreign", TeamStatus.active);
        when(events.findById(event.getId())).thenReturn(Optional.of(event));
        when(teams.findWithTrackById(foreignTeam.getId())).thenReturn(Optional.of(foreignTeam));

        assertEquals(400, assertThrows(ApiException.class, () -> service.decide(
                event.getId(), foreignTeam.getId(),
                new SeedingDtos.SeedDecisionRequest("confirmed", 1, null, null, null), auth))
                .getStatus().value());
        verify(assignments, never()).save(any());
    }

    @Test
    void coordinatorDecisionIsAuditedAndDoesNotMutateHistoricalFinish() {
        CandidateFixture f = candidateFixture(3, 1);
        when(teams.findWithTrackById(f.currentTeam.getId())).thenReturn(Optional.of(f.currentTeam));
        when(finishes.findById(f.finish.getId())).thenReturn(Optional.of(f.finish));
        when(assignments.findByEventIdAndTeamIdAndCompetitionStage(
                f.currentEvent.getId(), f.currentTeam.getId(), SeedingService.STAGE))
                .thenReturn(Optional.empty());
        when(users.findById(coordinator.getId())).thenReturn(Optional.of(coordinator));
        when(assignments.saveAndFlush(any())).thenAnswer(invocation -> {
            EventSeedAssignment assignment = invocation.getArgument(0);
            assignment.setId(UUID.randomUUID());
            return assignment;
        });
        int historicalRank = f.finish.getFinalRank();

        var result = service.decide(f.currentEvent.getId(), f.currentTeam.getId(),
                new SeedingDtos.SeedDecisionRequest("confirmed", 1, null, f.finish.getId(), null), auth);

        assertEquals("confirmed", result.status());
        assertEquals("TIER_1", result.seedTier());
        assertEquals(historicalRank, f.finish.getFinalRank());
        verify(audits).save(argThat(log -> log.getAction() == AuditAction.ASSIGN_SEED));
    }

    @Test
    void overrideRequiresRationaleAndDuplicateSeedMapsToConflict() {
        CandidateFixture f = candidateFixture(3, 2);
        when(teams.findWithTrackById(f.currentTeam.getId())).thenReturn(Optional.of(f.currentTeam));
        assertEquals(400, assertThrows(ApiException.class, () -> service.decide(
                f.currentEvent.getId(), f.currentTeam.getId(),
                new SeedingDtos.SeedDecisionRequest("overridden", 2, "TIER_2", f.finish.getId(), " "), auth))
                .getStatus().value());

        when(finishes.findById(f.finish.getId())).thenReturn(Optional.of(f.finish));
        when(assignments.findByEventIdAndTeamIdAndCompetitionStage(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(users.findById(coordinator.getId())).thenReturn(Optional.of(coordinator));
        when(assignments.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException(
                "duplicate key violates constraint uq_event_seed_number_active"));
        assertEquals(409, assertThrows(ApiException.class, () -> service.decide(
                f.currentEvent.getId(), f.currentTeam.getId(),
                new SeedingDtos.SeedDecisionRequest("confirmed", 1, null, f.finish.getId(), null), auth))
                .getStatus().value());
    }

    private FinalFixture finalFixture() {
        Event event = event(EventStatus.ongoing, "Current Final");
        Track track = track(event, "AI");
        TeamProfile profile = profile("Persistent");
        Team team = team(track, profile, "Finalist", TeamStatus.active);
        Round round = Round.builder().track(track).name("Final").sequenceNumber(2)
                .lifecycleState(RoundLifecycleState.READY_FOR_AWARDS)
                .appealDeadline(LocalDateTime.now(clock).minusMinutes(1)).build();
        round.setId(UUID.randomUUID());
        RoundResultVersion version = RoundResultVersion.builder().round(round).versionNumber(1)
                .status("published").publishedAt(LocalDateTime.now(clock).minusMinutes(20))
                .appealDeadline(LocalDateTime.now(clock).minusMinutes(1))
                .createdAt(LocalDateTime.now(clock).minusMinutes(20)).build();
        version.setId(UUID.randomUUID());
        RoundResultVersionEntry entry = RoundResultVersionEntry.builder().resultVersion(version)
                .team(team).rank(3).totalScore(BigDecimal.TEN).promotionStatus("promoted")
                .createdAt(LocalDateTime.now(clock)).build();
        entry.setId(UUID.randomUUID());
        when(events.findById(event.getId())).thenReturn(Optional.of(event));
        lenient().when(users.findById(coordinator.getId())).thenReturn(Optional.of(coordinator));
        when(rounds.findByTrackEventId(eq(event.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(round)));
        when(lifecycle.refresh(round)).thenReturn(round);
        lenient().when(versions.findByRoundIdAndStatus(round.getId(), "published")).thenReturn(Optional.of(version));
        lenient().when(versionEntries.findByResultVersionId(version.getId())).thenReturn(List.of(entry));
        return new FinalFixture(event, track, profile, team, round, version, entry);
    }

    private CandidateFixture candidateFixture(int returning, int rank) {
        Event currentEvent = event(EventStatus.ongoing, "2027");
        Track currentTrack = track(currentEvent, "AI");
        TeamProfile profile = profile("Persistent Alpha");
        Team current = team(currentTrack, profile, "Current Identity Name", TeamStatus.active);

        Event historicalEvent = event(EventStatus.completed, "2026");
        Track historicalTrack = track(historicalEvent, "AI");
        Team historical = team(historicalTrack, profile, "Different Historical Name", TeamStatus.active);
        Round historicalRound = Round.builder().track(historicalTrack).name("Final").sequenceNumber(2).build();
        historicalRound.setId(UUID.randomUUID());
        RoundResultVersion version = RoundResultVersion.builder().round(historicalRound)
                .versionNumber(1).status("published").publishedAt(LocalDateTime.now(clock))
                .appealDeadline(LocalDateTime.now(clock)).createdAt(LocalDateTime.now(clock)).build();
        version.setId(UUID.randomUUID());
        EventTeamFinish finish = EventTeamFinish.builder().event(historicalEvent).track(historicalTrack)
                .team(historical).teamProfile(profile).finalRound(historicalRound).resultVersion(version)
                .finalRank(rank).completionStatus("completed").completedAt(LocalDateTime.now(clock)).build();
        finish.setId(UUID.randomUUID());

        List<User> oldUsers = List.of(user("One"), user("Two"), user("Three"), user("Four"));
        List<TeamMember> historicalRoster = oldUsers.stream().map(u -> member(historical, u)).toList();
        List<TeamMember> currentRoster = new ArrayList<>();
        for (int i = 0; i < returning; i++) currentRoster.add(member(current, oldUsers.get(i)));
        currentRoster.add(member(current, user("New")));

        when(events.findById(currentEvent.getId())).thenReturn(Optional.of(currentEvent));
        lenient().when(tracks.findById(currentTrack.getId())).thenReturn(Optional.of(currentTrack));
        when(teams.findByTrackEventId(currentEvent.getId())).thenReturn(List.of(current));
        when(finishes.findByTeamProfileIdIn(any())).thenReturn(List.of(finish));
        when(members.findByTeamIdIn(any())).thenReturn(join(currentRoster, historicalRoster));
        when(assignments.findByEventId(currentEvent.getId())).thenReturn(List.of());
        return new CandidateFixture(currentEvent, currentTrack, current, finish);
    }

    private <T> List<T> join(List<T> left, List<T> right) {
        List<T> result = new ArrayList<>(left);
        result.addAll(right);
        return result;
    }

    private EventTeamFinish finish(FinalFixture f, int rank, String status) {
        EventTeamFinish finish = EventTeamFinish.builder().event(f.event).track(f.track).team(f.team)
                .teamProfile(f.profile).finalRound(f.round).resultVersion(f.version)
                .finalRank(rank).completionStatus(status).completedAt(LocalDateTime.now(clock)).build();
        finish.setId(UUID.randomUUID());
        return finish;
    }

    private Event event(EventStatus status, String title) {
        Event event = Event.builder().title(title).status(status).build();
        event.setId(UUID.randomUUID());
        return event;
    }

    private Track track(Event event, String name) {
        Track track = Track.builder().event(event).name(name).build();
        track.setId(UUID.randomUUID());
        return track;
    }

    private TeamProfile profile(String name) {
        TeamProfile profile = TeamProfile.builder().canonicalName(name).status(TeamProfileStatus.active).build();
        profile.setId(UUID.randomUUID());
        return profile;
    }

    private Team team(Track track, TeamProfile profile, String name, TeamStatus status) {
        Team team = Team.builder().track(track).teamProfile(profile).name(name).status(status).build();
        team.setId(UUID.randomUUID());
        return team;
    }

    private User user(String name) {
        User user = User.builder().fullName(name).email(name.toLowerCase() + "@example.com")
                .passwordHash("x").status(AccountStatus.approved).build();
        user.setId(UUID.randomUUID());
        return user;
    }

    private TeamMember member(Team team, User user) {
        TeamMember member = TeamMember.builder().team(team).user(user).role(TeamMemberRole.member).build();
        member.setId(UUID.randomUUID());
        return member;
    }

    private record FinalFixture(Event event, Track track, TeamProfile profile, Team team, Round round,
                                RoundResultVersion version, RoundResultVersionEntry entry) {}
    private record CandidateFixture(Event currentEvent, Track currentTrack, Team currentTeam,
                                    EventTeamFinish finish) {}
}
