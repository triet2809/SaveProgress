package vn.edu.fpt.seal.modules.round;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import vn.edu.fpt.seal.common.enums.EventStatus;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.audit.repository.AuditLogRepository;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.round.dto.CreateLogicalRoundRequest;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.round.entity.RoundDefinition;
import vn.edu.fpt.seal.modules.round.repository.RoundDefinitionRepository;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;
import vn.edu.fpt.seal.modules.round.service.CompetitionLifecycleService;
import vn.edu.fpt.seal.modules.round.service.RoundService;
import vn.edu.fpt.seal.modules.timeline.service.TimelineService;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.track.repository.TrackRepository;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogicalRoundServiceTest {
    @Mock RoundRepository rounds;
    @Mock TrackRepository tracks;
    @Mock AuditLogRepository audits;
    @Mock UserRepository users;
    @Mock CompetitionLifecycleService lifecycle;
    @Mock TimelineService timeline;
    @Mock RoundDefinitionRepository definitions;

    RoundService service;
    Event event;
    Track first;
    Track second;

    @BeforeEach
    void setUp() {
        service = new RoundService(rounds, tracks, audits, users, lifecycle, timeline, definitions);
        event = Event.builder().title("Event").status(EventStatus.published).build();
        event.setId(UUID.randomUUID());
        first = track(event, "A");
        second = track(event, "B");
        lenient().when(definitions.saveAndFlush(any())).thenAnswer(invocation -> {
            RoundDefinition definition = invocation.getArgument(0);
            definition.setId(UUID.randomUUID());
            return definition;
        });
        lenient().when(rounds.saveAllAndFlush(any())).thenAnswer(invocation -> {
            List<Round> saved = new ArrayList<>();
            for (Round round : (Iterable<Round>) invocation.getArgument(0)) {
                round.setId(UUID.randomUUID());
                saved.add(round);
            }
            return saved;
        });
    }

    @Test
    void createsOneLogicalRoundForOneTrack() {
        when(tracks.findAllById(List.of(first.getId()))).thenReturn(List.of(first));

        var response = service.createLogical(request(List.of(first.getId())));

        assertEquals(1, response.trackRounds().size());
        assertEquals(response.logicalRoundId(), response.trackRounds().get(0).logicalRoundId());
    }

    @Test
    void createsOneLogicalRoundForSeveralTracks() {
        when(tracks.findAllById(List.of(first.getId(), second.getId()))).thenReturn(List.of(first, second));

        var response = service.createLogical(request(List.of(first.getId(), second.getId())));

        assertEquals(2, response.trackRounds().size());
        assertEquals(1, response.trackRounds().stream().map(round -> round.logicalRoundId()).distinct().count());
        assertEquals(Set.of(first.getId(), second.getId()), response.trackRounds().stream()
                .map(round -> round.trackId()).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void duplicateTrackSelectionIsRejected() {
        assertEquals(400, assertThrows(ApiException.class,
                () -> service.createLogical(request(List.of(first.getId(), first.getId())))).getStatus().value());
        verifyNoInteractions(definitions);
    }

    @Test
    void trackFromAnotherEventIsRejected() {
        Event other = Event.builder().title("Other").status(EventStatus.published).build();
        other.setId(UUID.randomUUID());
        Track foreign = track(other, "Foreign");
        when(tracks.findAllById(List.of(first.getId(), foreign.getId()))).thenReturn(List.of(first, foreign));

        assertEquals(400, assertThrows(ApiException.class,
                () -> service.createLogical(request(List.of(first.getId(), foreign.getId())))).getStatus().value());
        verify(definitions, never()).saveAndFlush(any());
    }

    @Test
    void completedEventBlocksLogicalRoundCreation() {
        event.setStatus(EventStatus.completed);
        when(tracks.findAllById(List.of(first.getId()))).thenReturn(List.of(first));

        assertThrows(ApiException.class, () -> service.createLogical(request(List.of(first.getId()))));
        verify(definitions, never()).saveAndFlush(any());
    }

    @Test
    void assignmentFailurePropagatesForTransactionalRollback() {
        when(tracks.findAllById(List.of(first.getId(), second.getId()))).thenReturn(List.of(first, second));
        doThrow(new DataIntegrityViolationException("assignment failed")).when(rounds).saveAllAndFlush(any());

        assertThrows(DataIntegrityViolationException.class,
                () -> service.createLogical(request(List.of(first.getId(), second.getId()))));
        verify(definitions).saveAndFlush(any());
    }

    private CreateLogicalRoundRequest request(List<UUID> trackIds) {
        return new CreateLogicalRoundRequest(trackIds, "Round 1", 1,
                LocalDateTime.now().plusDays(1), 3);
    }

    private Track track(Event owner, String name) {
        Track track = Track.builder().event(owner).name(name).build();
        track.setId(UUID.randomUUID());
        return track;
    }
}
