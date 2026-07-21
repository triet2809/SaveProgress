package vn.edu.fpt.seal.modules.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import vn.edu.fpt.seal.common.enums.*;
import vn.edu.fpt.seal.modules.audit.repository.AuditLogRepository;
import vn.edu.fpt.seal.modules.event.dto.*;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.event.repository.EventRepository;
import vn.edu.fpt.seal.modules.event.service.EventService;
import vn.edu.fpt.seal.modules.participant.repository.RoundParticipantRepository;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;
import vn.edu.fpt.seal.modules.round.repository.RoundDefinitionRepository;
import vn.edu.fpt.seal.modules.round.service.CompetitionLifecycleService;
import vn.edu.fpt.seal.modules.seeding.dto.SeedingDtos;
import vn.edu.fpt.seal.modules.seeding.entity.EventSeedAssignment;
import vn.edu.fpt.seal.modules.seeding.service.SeedingService;
import vn.edu.fpt.seal.modules.recognition.service.TeamRecognitionService;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.team.repository.*;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.track.repository.TrackRepository;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.modules.timeline.service.TimelineService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventSeedingSetupTest {
    @Mock EventRepository events;
    @Mock TrackRepository tracks;
    @Mock RoundRepository rounds;
    @Mock TeamRepository teams;
    @Mock RoundParticipantRepository participants;
    @Mock TeamMemberRepository members;
    @Mock AuditLogRepository audits;
    @Mock UserRepository users;
    @Mock CompetitionLifecycleService lifecycle;
    @Mock SeedingService seeding;
    @Mock TeamRecognitionService recognition;
    @Mock TimelineService timeline;
    @Mock RoundDefinitionRepository definitions;

    private EventService service;

    @BeforeEach
    void setUp() {
        service = new EventService(events, tracks, rounds, teams, participants,
                members, audits, users, lifecycle, seeding, recognition, timeline, definitions);
        lenient().when(definitions.save(any())).thenAnswer(invocation -> {
            var definition = invocation.getArgument(0, vn.edu.fpt.seal.modules.round.entity.RoundDefinition.class);
            definition.setId(UUID.randomUUID());
            return definition;
        });
    }

    @Test
    void eventCompletionCannotBypassHistoricalSnapshotGate() {
        Event event = event();
        when(events.findById(event.getId())).thenReturn(Optional.of(event));

        service.changeStatus(event.getId(), EventStatus.completed);

        verify(lifecycle).requireAwardsAllowed(event.getId());
        verify(seeding).requireEventFinalized(event.getId());
        verify(recognition).evaluateProfiles(any(), isNull());
        assertEquals(EventStatus.completed, event.getStatus());
    }

    @Test
    void confirmedSeedTrackSurvivesSetupAndUnreviewedCandidatesProduceWarning() {
        Event event = event();
        Track track = Track.builder().event(event).name("General").build();
        track.setId(UUID.randomUUID());
        Team team = Team.builder().track(track).name("Seeded").status(TeamStatus.active).build();
        team.setId(UUID.randomUUID());
        EventSeedAssignment seed = EventSeedAssignment.builder().event(event).track(track).team(team)
                .competitionStage(SeedingService.STAGE).status("confirmed").continuityCount(3).build();
        var review = new SeedingDtos.ReviewSummary(1, 0, 0, 0, 1,
                "1 eligible seed candidate(s) remain unreviewed.");

        when(events.findById(event.getId())).thenReturn(Optional.of(event));
        when(rounds.countByTrackEventId(event.getId())).thenReturn(0L);
        when(seeding.setupReview(event.getId())).thenReturn(review);
        when(seeding.confirmedAssignments(event.getId())).thenReturn(List.of(seed));
        when(teams.findByTrackEventId(event.getId())).thenReturn(List.of(team));
        when(tracks.findByEventId(eq(event.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(track)));
        when(rounds.save(any())).thenAnswer(invocation -> {
            Round round = invocation.getArgument(0);
            round.setId(UUID.randomUUID());
            return round;
        });

        SetupCompetitionResponse response = service.setupCompetition(event.getId(),
                new SetupCompetitionRequest(null, null, null, List.of(
                        new SetupCompetitionRequest.LogicalRoundSpec("Final", 1, true, 1, List.of(
                                new SetupCompetitionRequest.RoundTrackSpec("General", null, 1))))));

        assertSame(track, team.getTrack());
        assertEquals(1, response.seedReview().unreviewed());
        assertFalse(response.warnings().isEmpty());
        verify(teams).saveAll(List.of(team));
    }

    @Test
    void eventLevelPlanSupportsDifferentTrackSetsAndSingleTrackFinal() {
        Event event = event();
        Track general = Track.builder().event(event).name("General").build();
        general.setId(UUID.randomUUID());
        Team team = Team.builder().track(general).name("Team One").status(TeamStatus.active).build();
        team.setId(UUID.randomUUID());
        when(events.findById(event.getId())).thenReturn(Optional.of(event));
        when(rounds.countByTrackEventId(event.getId())).thenReturn(0L);
        when(seeding.setupReview(event.getId())).thenReturn(
                new SeedingDtos.ReviewSummary(0, 0, 0, 0, 0, null));
        when(seeding.confirmedAssignments(event.getId())).thenReturn(List.of());
        when(teams.findByTrackEventId(event.getId())).thenReturn(List.of(team));
        when(tracks.findByEventId(eq(event.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(general)));
        when(tracks.save(any())).thenAnswer(invocation -> {
            Track track = invocation.getArgument(0);
            track.setId(UUID.randomUUID());
            return track;
        });
        when(rounds.save(any())).thenAnswer(invocation -> {
            Round round = invocation.getArgument(0);
            round.setId(UUID.randomUUID());
            return round;
        });

        SetupCompetitionRequest request = new SetupCompetitionRequest(null, null, null, List.of(
                new SetupCompetitionRequest.LogicalRoundSpec("Qualifier", 1, false, 4, List.of(
                        new SetupCompetitionRequest.RoundTrackSpec("General", null, 3),
                        new SetupCompetitionRequest.RoundTrackSpec("AI", null, 2))),
                new SetupCompetitionRequest.LogicalRoundSpec("Final", 2, true, 1, List.of(
                        new SetupCompetitionRequest.RoundTrackSpec("Final Stage", null, 1)))));

        SetupCompetitionResponse response = service.setupCompetition(event.getId(), request);

        assertEquals(3, response.trackCount());
        assertEquals(2, response.roundsPerTrack());
        verify(definitions, times(2)).save(any());
        verify(rounds, times(3)).save(any());
        verify(participants, times(1)).save(any());
    }

    private Event event() {
        Event event = Event.builder().title("Current").status(EventStatus.ongoing).build();
        event.setId(UUID.randomUUID());
        return event;
    }
}
