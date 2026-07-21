package vn.edu.fpt.seal.modules.appeal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.ArgumentCaptor;
import vn.edu.fpt.seal.common.enums.TeamMemberRole;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.appeal.dto.AppealDtos;
import vn.edu.fpt.seal.modules.appeal.repository.AppealRepository;
import vn.edu.fpt.seal.modules.appeal.entity.Appeal;
import vn.edu.fpt.seal.modules.appeal.service.AppealService;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.participant.repository.RoundParticipantRepository;
import vn.edu.fpt.seal.modules.resultversion.entity.RoundResultVersion;
import vn.edu.fpt.seal.modules.resultversion.repository.RoundResultVersionRepository;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;
import vn.edu.fpt.seal.modules.round.service.CompetitionLifecycleService;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.team.entity.TeamMember;
import vn.edu.fpt.seal.modules.team.repository.TeamMemberRepository;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.modules.user.entity.User;
import vn.edu.fpt.seal.security.CurrentUser;
import vn.edu.fpt.seal.modules.timeline.TimelineEventType;
import vn.edu.fpt.seal.modules.timeline.TimelineScope;
import vn.edu.fpt.seal.modules.timeline.dto.TimelineEventRequest;
import vn.edu.fpt.seal.modules.timeline.service.TimelineService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppealServiceTest {
    @Mock AppealRepository appeals;
    @Mock RoundRepository rounds;
    @Mock RoundResultVersionRepository versions;
    @Mock RoundParticipantRepository participants;
    @Mock TeamMemberRepository members;
    @Mock UserRepository users;
    @Mock CompetitionLifecycleService lifecycle;
    @Mock TimelineService timeline;

    private AppealService service;
    private CurrentUser currentUser;
    private UsernamePasswordAuthenticationToken authentication;
    private Round round;
    private Team team;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-19T10:00:00Z"), ZoneOffset.UTC);
        service = new AppealService(appeals, rounds, versions, participants, members, users, lifecycle, clock);
        ReflectionTestUtils.setField(service, "timeline", timeline);
        UUID userId = UUID.randomUUID();
        currentUser = CurrentUser.builder().id(userId).roles(List.of("team_member")).build();
        authentication = new UsernamePasswordAuthenticationToken(currentUser, null);
        Event event = Event.builder().title("Hackathon").build();
        event.setId(UUID.randomUUID());
        Track track = Track.builder().event(event).name("General").build();
        track.setId(UUID.randomUUID());
        round = Round.builder().track(track).name("Final").sequenceNumber(1).build();
        round.setId(UUID.randomUUID());
        team = Team.builder().track(track).name("Alpha").build();
        team.setId(UUID.randomUUID());
    }

    @Test
    void acceptedAppealRequiringRecalculationEmitsPrivateSafeTimelineEvents() {
        CurrentUser coordinator = CurrentUser.builder().id(currentUser.getId()).roles(List.of("coordinator")).build();
        Authentication coordinatorAuth = new UsernamePasswordAuthenticationToken(coordinator, null);
        User actor = new User(); actor.setId(currentUser.getId());
        Appeal appeal = Appeal.builder().event(round.getTrack().getEvent()).round(round).team(team)
                .submittedBy(actor).status("PENDING").reason("private reason").build();
        appeal.setId(UUID.randomUUID());
        when(appeals.findWithRelationsById(appeal.getId())).thenReturn(Optional.of(appeal));
        when(users.findById(currentUser.getId())).thenReturn(Optional.of(actor));
        service.resolve(appeal.getId(), new AppealDtos.Resolve("ACCEPTED", "private response", true), coordinatorAuth);

        ArgumentCaptor<TimelineEventRequest> captured = ArgumentCaptor.forClass(TimelineEventRequest.class);
        verify(timeline, times(3)).record(captured.capture());
        assertEquals(TimelineEventType.APPEAL_ACCEPTED_RECALCULATION, captured.getAllValues().get(0).eventType());
        assertEquals(TimelineScope.TEAM_PRIVATE, captured.getAllValues().get(0).visibility());
        assertEquals(TimelineEventType.RESULT_RECALCULATION_REQUIRED, captured.getAllValues().get(2).eventType());
        assertEquals(TimelineScope.COORDINATOR_PRIVATE, captured.getAllValues().get(2).visibility());
        assertTrue(captured.getAllValues().stream().noneMatch(r -> r.description().contains("private reason")));
    }

    @Test
    void nonLeaderCannotSubmitAppeal() {
        TeamMember member = TeamMember.builder().team(team).role(TeamMemberRole.member).build();
        when(rounds.findById(round.getId())).thenReturn(Optional.of(round));
        when(members.findByUserIdOrderByJoinedAtDesc(currentUser.getId())).thenReturn(List.of(member));

        ApiException exception = assertThrows(ApiException.class,
                () -> service.create(new AppealDtos.Create(round.getId(), "Please review"), authentication));

        assertEquals(403, exception.getStatus().value());
        verifyNoInteractions(versions, appeals);
    }

    @Test
    void exactServerDeadlineIsExpired() {
        TeamMember leader = TeamMember.builder().team(team).role(TeamMemberRole.leader).build();
        RoundResultVersion version = RoundResultVersion.builder().round(round).versionNumber(1)
                .status("published").publishedAt(LocalDateTime.of(2026, 7, 19, 9, 45))
                .appealDeadline(LocalDateTime.of(2026, 7, 19, 10, 0)).build();
        version.setId(UUID.randomUUID());
        when(rounds.findById(round.getId())).thenReturn(Optional.of(round));
        when(members.findByUserIdOrderByJoinedAtDesc(currentUser.getId())).thenReturn(List.of(leader));
        when(participants.existsByRoundIdAndTeamId(round.getId(), team.getId())).thenReturn(true);
        when(versions.findByRoundIdAndStatus(round.getId(), "published")).thenReturn(Optional.of(version));

        ApiException exception = assertThrows(ApiException.class,
                () -> service.create(new AppealDtos.Create(round.getId(), "Please review"), authentication));

        assertEquals(400, exception.getStatus().value());
        assertTrue(exception.getMessage().contains("expired"));
        verify(appeals, never()).save(any());
    }
}
