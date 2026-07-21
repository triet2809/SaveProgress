package vn.edu.fpt.seal.modules.timeline;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.event.repository.EventRepository;
import vn.edu.fpt.seal.modules.timeline.dto.TimelineEventRequest;
import vn.edu.fpt.seal.modules.timeline.entity.TimelineEvent;
import vn.edu.fpt.seal.modules.timeline.repository.TimelineEventRepository;
import vn.edu.fpt.seal.modules.timeline.service.TimelineService;
import vn.edu.fpt.seal.security.AuthorizationService;
import vn.edu.fpt.seal.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.*;
import vn.edu.fpt.seal.common.enums.EventStatus;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.team.entity.Team;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimelineServiceTest {
    @Mock TimelineEventRepository repository;
    @Mock EventRepository events;
    @Mock vn.edu.fpt.seal.modules.team.repository.TeamMemberRepository members;
    @Mock vn.edu.fpt.seal.modules.team.repository.TeamRepository teams;
    @Mock vn.edu.fpt.seal.modules.round.repository.RoundRepository rounds;
    @Mock vn.edu.fpt.seal.modules.track.repository.TrackRepository tracks;
    @Mock AuthorizationService authorization;
    @Mock Authentication authentication;
    Clock clock = Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC);
    TimelineService service;

    @BeforeEach void setup() {
        service = new TimelineService(repository, events, members, teams, rounds, tracks, authorization, clock);
    }

    @Test
    void recordIsIdempotentAndSanitizesSensitiveText() {
        UUID eventId=UUID.randomUUID(), source=UUID.randomUUID(); Event event=new Event(); event.setId(eventId);
        when(events.findById(eventId)).thenReturn(Optional.of(event));
        when(repository.findByIdempotencyKey("event:"+source)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any())).thenAnswer(inv->inv.getArgument(0));
        TimelineEvent result=service.record(new TimelineEventRequest(eventId,null,null,null,
                TimelineEventType.EVENT_PUBLISHED,TimelineSourceType.EVENT,source,TimelineScope.EVENT_PUBLIC,
                " Open\r\nregistration ","token=abc password:bad",null,"event:"+source));
        assertEquals("Open registration",result.getTitle());
        assertFalse(result.getDescription().contains("abc"));
        assertFalse(result.getDescription().contains("bad"));
    }

    @Test
    void duplicateRetryReturnsOneEntry() {
        TimelineEvent existing=TimelineEvent.builder().idempotencyKey("same").build();
        when(repository.findByIdempotencyKey("same")).thenReturn(Optional.of(existing));
        assertSame(existing,service.record(request("same")));
        verify(repository,never()).saveAndFlush(any());
    }

    @Test
    void concurrentUniqueConflictCreatesAtMostOneAndRollsBackLoser() {
        UUID eventId=UUID.randomUUID(); Event event=new Event(); event.setId(eventId);
        when(repository.findByIdempotencyKey("same")).thenReturn(Optional.empty());
        when(events.findById(eventId)).thenReturn(Optional.of(event));
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException(
                "duplicate key violates constraint uq_team_timeline_events_idempotency"));
        TimelineEventRequest request=new TimelineEventRequest(eventId,null,null,null,TimelineEventType.EVENT_PUBLISHED,
                TimelineSourceType.EVENT,eventId,TimelineScope.EVENT_PUBLIC,"Published",null,null,"same");
        assertThrows(vn.edu.fpt.seal.common.exception.ApiException.class,()->service.record(request));
        verify(repository,times(1)).saveAndFlush(any());
    }

    @Test
    void nonDuplicateTimelineFailurePropagatesToCallingTransaction() {
        UUID eventId=UUID.randomUUID(); Event event=event(eventId,EventStatus.ongoing);
        when(events.findById(eventId)).thenReturn(Optional.of(event));
        when(repository.findByIdempotencyKey("failure")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any())).thenThrow(new IllegalStateException("storage unavailable"));
        TimelineEventRequest request=new TimelineEventRequest(eventId,null,null,null,TimelineEventType.EVENT_STARTED,
                TimelineSourceType.EVENT,eventId,TimelineScope.EVENT_PUBLIC,"Event started",null,null,"failure");
        assertThrows(IllegalStateException.class,()->service.record(request));
    }

    @Test
    void anonymousSeesOnlyPublicEntriesWithoutPrivateCountLeak() {
        UUID eventId=UUID.randomUUID(); Event event=event(eventId,EventStatus.published);
        TimelineEvent publicItem=item(event,TimelineScope.EVENT_PUBLIC);
        TimelineEvent privateItem=item(event,TimelineScope.COORDINATOR_PRIVATE);
        when(events.findById(eventId)).thenReturn(Optional.of(event));
        when(repository.search(eq(eventId),isNull(),isNull(),isNull(),eq(TimelineScope.EVENT_PUBLIC),any()))
                .thenReturn(new PageImpl<>(List.of(publicItem)));
        Page<?> result=service.event(eventId,null,null,null,null,Pageable.unpaged(),null);
        assertEquals(1,result.getContent().size());
        assertEquals(1,result.getTotalElements());
    }

    @Test
    void participantSeesParticipantButNotCoordinatorPrivate() {
        UUID eventId=UUID.randomUUID(),userId=UUID.randomUUID(); Event event=event(eventId,EventStatus.ongoing);
        CurrentUser user=CurrentUser.builder().id(userId).roles(List.of("participant")).build();
        when(authentication.getPrincipal()).thenReturn(user);
        when(events.findById(eventId)).thenReturn(Optional.of(event));
        when(members.existsActiveRegistrationInEvent(userId,eventId)).thenReturn(true);
        when(repository.search(eq(eventId),isNull(),isNull(),isNull(),isNull(),any())).thenReturn(new PageImpl<>(List.of(
                item(event,TimelineScope.EVENT_PARTICIPANTS),item(event,TimelineScope.COORDINATOR_PRIVATE))));
        assertEquals(1,service.event(eventId,null,null,null,null,Pageable.unpaged(),authentication).getNumberOfElements());
    }

    @Test
    void assignedJudgeSeesRelevantStaffPrivateAndUnassignedJudgeDoesNot() {
        UUID eventId=UUID.randomUUID(),userId=UUID.randomUUID(),roundId=UUID.randomUUID();
        Event event=event(eventId,EventStatus.ongoing); Track track=Track.builder().event(event).name("Track").build(); track.setId(UUID.randomUUID());
        Round round=Round.builder().track(track).name("Round").build(); round.setId(roundId);
        TimelineEvent staff=TimelineEvent.builder().id(UUID.randomUUID()).event(event).track(track).round(round)
                .eventType(TimelineEventType.INCIDENT_SUBMITTED).visibilityScope(TimelineScope.STAFF_PRIVATE)
                .title("Incident submitted").occurredAt(LocalDateTime.now(clock)).build();
        CurrentUser user=CurrentUser.builder().id(userId).roles(List.of("judge")).build();
        when(authentication.getPrincipal()).thenReturn(user); when(events.findById(eventId)).thenReturn(Optional.of(event));
        when(repository.search(eq(eventId),isNull(),isNull(),isNull(),isNull(),any())).thenReturn(new PageImpl<>(List.of(staff)));
        when(authorization.isAssignedJudge(user,roundId)).thenReturn(true);
        assertEquals(1,service.event(eventId,null,null,null,null,Pageable.unpaged(),authentication).getNumberOfElements());
        when(authorization.isAssignedJudge(user,roundId)).thenReturn(false);
        assertEquals(0,service.event(eventId,null,null,null,null,Pageable.unpaged(),authentication).getNumberOfElements());
    }

    @Test
    void coordinatorSeesCoordinatorPrivate() {
        UUID eventId=UUID.randomUUID(); Event event=event(eventId,EventStatus.ongoing);
        CurrentUser user=CurrentUser.builder().id(UUID.randomUUID()).roles(List.of("coordinator")).build();
        when(authentication.getPrincipal()).thenReturn(user); when(events.findById(eventId)).thenReturn(Optional.of(event));
        when(authorization.isCoordinator(user)).thenReturn(true);
        when(repository.search(eq(eventId),isNull(),isNull(),isNull(),isNull(),any())).thenReturn(new PageImpl<>(List.of(item(event,TimelineScope.COORDINATOR_PRIVATE))));
        assertEquals(1,service.event(eventId,null,null,null,null,Pageable.unpaged(),authentication).getNumberOfElements());
    }

    private TimelineEventRequest request(String key) {
        return new TimelineEventRequest(UUID.randomUUID(),null,null,null,TimelineEventType.EVENT_PUBLISHED,
                TimelineSourceType.EVENT,UUID.randomUUID(),TimelineScope.EVENT_PUBLIC,"Published",null,null,key);
    }
    private Event event(UUID id,EventStatus status){Event event=new Event();event.setId(id);event.setStatus(status);return event;}
    private TimelineEvent item(Event event,TimelineScope scope){return TimelineEvent.builder().id(UUID.randomUUID()).event(event)
            .eventType(TimelineEventType.EVENT_PUBLISHED).visibilityScope(scope).title("Safe").occurredAt(LocalDateTime.now(clock)).build();}
}
