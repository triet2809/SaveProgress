package vn.edu.fpt.seal.modules.round;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.audit.repository.AuditLogRepository;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;
import vn.edu.fpt.seal.modules.round.service.RoundService;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.track.repository.TrackRepository;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.modules.round.service.CompetitionLifecycleService;
import vn.edu.fpt.seal.modules.resultversion.entity.RoundResultVersion;
import vn.edu.fpt.seal.modules.timeline.TimelineEventType;
import vn.edu.fpt.seal.modules.timeline.service.TimelineService;

import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoundServicePublicationTest {
    @Mock RoundRepository rounds;
    @Mock TrackRepository tracks;
    @Mock AuditLogRepository audits;
    @Mock UserRepository users;
    @Mock CompetitionLifecycleService lifecycle;
    @Mock TimelineService timeline;

    @Test
    void successfulPublicationEmitsResultAndAppealWindowEvents() {
        RoundService service = new RoundService(rounds, tracks, audits, users, lifecycle, timeline);
        UUID eventId = UUID.randomUUID(), roundId = UUID.randomUUID();
        Event event = Event.builder().build(); event.setId(eventId);
        Track track = Track.builder().event(event).name("General").build();
        Round round = Round.builder().track(track).name("Final").sequenceNumber(1).build(); round.setId(roundId);
        RoundResultVersion version = RoundResultVersion.builder().versionNumber(1).build(); version.setId(UUID.randomUUID());
        when(rounds.findById(roundId)).thenReturn(java.util.Optional.of(round));
        when(lifecycle.publish(eq(round), isNull(), anyString())).thenReturn(version);
        service.publishResults(eventId, roundId, null);

        ArgumentCaptor<vn.edu.fpt.seal.modules.timeline.dto.TimelineEventRequest> requests =
                ArgumentCaptor.forClass(vn.edu.fpt.seal.modules.timeline.dto.TimelineEventRequest.class);
        verify(timeline, times(2)).record(requests.capture());
        assertEquals(TimelineEventType.RESULT_PUBLISHED, requests.getAllValues().get(0).eventType());
        assertEquals(TimelineEventType.APPEAL_WINDOW_OPENED, requests.getAllValues().get(1).eventType());
    }

    @Test
    void publishesOnceAndIsIdempotent() {
        RoundService service = new RoundService(rounds, tracks, audits, users);
        UUID eventId = UUID.randomUUID(), roundId = UUID.randomUUID();
        Event event = Event.builder().build();
        event.setId(eventId);
        Track track = Track.builder().event(event).name("General").build();
        Round round = Round.builder().track(track).name("Final").sequenceNumber(1).build();
        round.setId(roundId);
        when(rounds.findById(roundId)).thenReturn(java.util.Optional.of(round));

        service.publishResults(eventId, roundId, null);
        service.publishResults(eventId, roundId, null);

        assertNotNull(round.getResultPublishedAt());
        verify(audits, times(1)).save(any());
    }

    @Test
    void rejectsRoundFromAnotherEvent() {
        RoundService service = new RoundService(rounds, tracks, audits, users);
        Event event = Event.builder().build();
        event.setId(UUID.randomUUID());
        Round round = Round.builder().track(Track.builder().event(event).build()).build();
        when(rounds.findById(any())).thenReturn(java.util.Optional.of(round));
        assertThrows(ApiException.class, () -> service.publishResults(UUID.randomUUID(), UUID.randomUUID(), null));
        verifyNoInteractions(audits);
    }
}
