package vn.edu.fpt.seal.modules.prize;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.event.repository.EventRepository;
import vn.edu.fpt.seal.modules.prize.dto.CreatePrizeRequest;
import vn.edu.fpt.seal.modules.prize.dto.UpdatePrizeRequest;
import vn.edu.fpt.seal.modules.prize.entity.Prize;
import vn.edu.fpt.seal.modules.prize.repository.PrizeRepository;
import vn.edu.fpt.seal.modules.prize.service.PrizeService;
import vn.edu.fpt.seal.modules.round.service.CompetitionLifecycleService;
import vn.edu.fpt.seal.modules.seeding.service.SeedingService;
import vn.edu.fpt.seal.modules.team.repository.TeamRepository;
import vn.edu.fpt.seal.modules.timeline.TimelineEventType;
import vn.edu.fpt.seal.modules.timeline.TimelineScope;
import vn.edu.fpt.seal.modules.timeline.dto.TimelineEventRequest;
import vn.edu.fpt.seal.modules.timeline.service.TimelineService;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.track.repository.TrackRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrizeServiceTest {
    @Mock PrizeRepository prizes;
    @Mock EventRepository events;
    @Mock TrackRepository tracks;
    @Mock TeamRepository teams;
    @Mock CompetitionLifecycleService lifecycle;
    @Mock SeedingService seeding;
    @Mock TimelineService timeline;

    private PrizeService service;
    private UUID eventId;
    private UUID prizeId;
    private Event event;

    @BeforeEach
    void setUp() {
        service = new PrizeService(prizes, events, tracks, teams, lifecycle, seeding);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "timeline", timeline);
        eventId = UUID.randomUUID();
        prizeId = UUID.randomUUID();
        event = new Event();
        event.setId(eventId);
        lenient().when(events.findById(eventId)).thenReturn(Optional.of(event));
        lenient().when(prizes.save(any(Prize.class))).thenAnswer(invocation -> {
            Prize prize = invocation.getArgument(0);
            prize.setId(prizeId);
            return prize;
        });
    }

    @Test
    void initialConfigurationCreatesOneStableTimelineEntry() {
        service.create(new CreatePrizeRequest(eventId, null, null, "Winner", new BigDecimal("100.00"), "Public prize", null));

        ArgumentCaptor<TimelineEventRequest> captor = ArgumentCaptor.forClass(TimelineEventRequest.class);
        verify(timeline, times(1)).record(captor.capture());
        TimelineEventRequest request = captor.getValue();
        assertEquals(TimelineEventType.PRIZE_CONFIGURED, request.eventType());
        assertEquals(TimelineScope.COORDINATOR_PRIVATE, request.visibility());
        assertEquals("prize:" + prizeId + ":configured", request.idempotencyKey());
        assertEquals("A coordinator configured a prize", request.description());
    }

    @Test
    void retryUsesSameStableIdempotencyKey() {
        service.create(new CreatePrizeRequest(eventId, null, null, "Winner", new BigDecimal("100.00"), "Public prize", null));
        service.create(new CreatePrizeRequest(eventId, null, null, "Winner", new BigDecimal("100.00"), "Public prize", null));

        ArgumentCaptor<TimelineEventRequest> captor = ArgumentCaptor.forClass(TimelineEventRequest.class);
        verify(timeline, times(2)).record(captor.capture());
        assertEquals("prize:" + prizeId + ":configured", captor.getAllValues().get(0).idempotencyKey());
        assertEquals("prize:" + prizeId + ":configured", captor.getAllValues().get(1).idempotencyKey());
    }

    @Test
    void materiallyDifferentUpdateIntentionallyCreatesNoTimelineEntry() {
        Prize prize = Prize.builder().id(prizeId).event(event).name("Winner").prizeAmount(new BigDecimal("100.00")).build();
        when(prizes.findWithRelationsById(prizeId)).thenReturn(Optional.of(prize));

        service.update(prizeId, new UpdatePrizeRequest(null, null, "Grand Winner", new BigDecimal("250.00"), "Changed", null));

        verifyNoInteractions(timeline);
        assertEquals("Grand Winner", prize.getName());
        assertEquals(new BigDecimal("250.00"), prize.getPrizeAmount());
    }

    @Test
    void lifecycleBlockedUpdateCreatesNoTimelineEntry() {
        Prize prize = Prize.builder().id(prizeId).event(event).name("Winner").build();
        when(prizes.findWithRelationsById(prizeId)).thenReturn(Optional.of(prize));
        doThrow(ApiException.conflict("Awards are not allowed")).when(lifecycle).requireAwardsAllowed(eventId);

        assertThrows(ApiException.class,
                () -> service.update(prizeId, new UpdatePrizeRequest(null, null, "Changed", null, null, null)));
        verifyNoInteractions(timeline);
    }

    @Test
    void invalidScopeUpdateAfterLifecycleCheckCreatesNoTimelineEntry() {
        UUID trackId = UUID.randomUUID();
        Event otherEvent = new Event();
        otherEvent.setId(UUID.randomUUID());
        Track foreignTrack = mock(Track.class);
        when(foreignTrack.getEvent()).thenReturn(otherEvent);
        when(tracks.findById(trackId)).thenReturn(Optional.of(foreignTrack));
        Prize prize = Prize.builder().id(prizeId).event(event).name("Winner").build();
        when(prizes.findWithRelationsById(prizeId)).thenReturn(Optional.of(prize));

        assertThrows(ApiException.class,
                () -> service.update(prizeId, new UpdatePrizeRequest(trackId, null, null, null, null, null)));
        verifyNoInteractions(timeline);
    }

    @Test
    void timelineDescriptionContainsNoSensitivePrizeDetails() {
        service.create(new CreatePrizeRequest(eventId, null, null, "Winner", new BigDecimal("999.00"),
                "Bank account 12345 internal payment note", null));

        ArgumentCaptor<TimelineEventRequest> captor = ArgumentCaptor.forClass(TimelineEventRequest.class);
        verify(timeline).record(captor.capture());
        String description = captor.getValue().description();
        assertFalse(description.contains("999"));
        assertFalse(description.toLowerCase().contains("payment"));
        assertFalse(description.toLowerCase().contains("internal"));
    }
}
