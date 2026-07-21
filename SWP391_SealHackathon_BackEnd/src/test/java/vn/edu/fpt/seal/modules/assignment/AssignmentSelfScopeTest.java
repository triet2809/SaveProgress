package vn.edu.fpt.seal.modules.assignment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.judge.repository.RoundJudgeRepository;
import vn.edu.fpt.seal.modules.judge.service.RoundJudgeService;
import vn.edu.fpt.seal.modules.mentor.repository.TrackMentorRepository;
import vn.edu.fpt.seal.modules.mentor.service.TrackMentorService;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;
import vn.edu.fpt.seal.modules.track.repository.TrackRepository;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.security.AuthorizationService;
import vn.edu.fpt.seal.security.CurrentUser;

import java.util.List;
import java.util.UUID;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.round.entity.Round;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentSelfScopeTest {
    @Mock RoundJudgeRepository roundJudges;
    @Mock RoundRepository rounds;
    @Mock TrackMentorRepository trackMentors;
    @Mock TrackRepository tracks;
    @Mock UserRepository users;
    @Mock AuthorizationService authorization;

    @Test
    void judgeCannotRequestAnotherJudgesSubmissions() {
        CurrentUser current = CurrentUser.builder().id(UUID.randomUUID()).roles(List.of("judge")).build();
        var authentication = new UsernamePasswordAuthenticationToken(current, null);
        when(authorization.current(authentication)).thenReturn(current);
        RoundJudgeService service = new RoundJudgeService(roundJudges, rounds, users, authorization);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.submissions(UUID.randomUUID(), null, null, null, authentication));
        assertEquals(403, exception.getStatus().value());
    }

    @Test
    void mentorCannotRequestAnotherMentorsTeams() {
        CurrentUser current = CurrentUser.builder().id(UUID.randomUUID()).roles(List.of("mentor")).build();
        var authentication = new UsernamePasswordAuthenticationToken(current, null);
        when(authorization.current(authentication)).thenReturn(current);
        TrackMentorService service = new TrackMentorService(trackMentors, tracks, users, authorization, rounds);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.teams(UUID.randomUUID(), null, null, null, authentication));
        assertEquals(403, exception.getStatus().value());
    }

    @Test
    void mentorEventQueryRemainsPrincipalScoped() {
        CurrentUser current = CurrentUser.builder().id(UUID.randomUUID()).roles(List.of("mentor")).build();
        var authentication = new UsernamePasswordAuthenticationToken(current, null);
        when(authorization.current(authentication)).thenReturn(current);
        when(authorization.hasRole(current, "mentor")).thenReturn(true);
        UUID eventId = UUID.randomUUID();
        when(trackMentors.findByEventIdAndUserId(eq(eventId), eq(current.getId()), any())).thenReturn(Page.empty());
        TrackMentorService service = new TrackMentorService(trackMentors, tracks, users, authorization, rounds);
        service.list(eventId, null, null, PageRequest.of(0, 20), authentication);
        verify(trackMentors).findByEventIdAndUserId(eq(eventId), eq(current.getId()), any());
    }

    @Test
    void mentorRejectsTrackFromAnotherEvent() {
        UUID requestedEvent = UUID.randomUUID();
        Event actualEvent = Event.builder().build(); actualEvent.setId(UUID.randomUUID());
        Track track = Track.builder().event(actualEvent).build(); track.setId(UUID.randomUUID());
        when(tracks.findById(track.getId())).thenReturn(Optional.of(track));
        TrackMentorService service = new TrackMentorService(trackMentors, tracks, users, authorization, rounds);
        assertThrows(ApiException.class, () -> service.list(requestedEvent, track.getId(), null,
                PageRequest.of(0, 20), new UsernamePasswordAuthenticationToken("coordinator", null)));
    }

    @Test
    void judgeRejectsCrossEventRoundTrackHierarchy() {
        Event event = Event.builder().build(); event.setId(UUID.randomUUID());
        Track track = Track.builder().event(event).build(); track.setId(UUID.randomUUID());
        Round round = Round.builder().track(track).build(); round.setId(UUID.randomUUID());
        when(rounds.findById(round.getId())).thenReturn(Optional.of(round));
        RoundJudgeService service = new RoundJudgeService(roundJudges, rounds, users, authorization);
        assertThrows(ApiException.class, () -> service.list(UUID.randomUUID(), round.getId(), track.getId(), null,
                PageRequest.of(0, 20), new UsernamePasswordAuthenticationToken("coordinator", null)));
    }
}
