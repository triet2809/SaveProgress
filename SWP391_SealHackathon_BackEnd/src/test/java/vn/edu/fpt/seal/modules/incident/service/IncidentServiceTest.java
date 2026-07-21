package vn.edu.fpt.seal.modules.incident.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import vn.edu.fpt.seal.common.enums.IncidentType;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.event.repository.EventRepository;
import vn.edu.fpt.seal.modules.incident.dto.CreateIncidentRequest;
import vn.edu.fpt.seal.modules.incident.repository.*;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;
import vn.edu.fpt.seal.modules.submission.repository.SubmissionRepository;
import vn.edu.fpt.seal.modules.team.repository.TeamRepository;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.track.repository.TrackRepository;
import vn.edu.fpt.seal.modules.user.entity.User;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.security.AuthorizationService;
import vn.edu.fpt.seal.security.CurrentUser;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {
    @Mock IncidentReportRepository reports;
    @Mock IncidentEvidenceRepository evidences;
    @Mock IncidentActionRepository actions;
    @Mock EventRepository events;
    @Mock TrackRepository tracks;
    @Mock RoundRepository rounds;
    @Mock TeamRepository teams;
    @Mock SubmissionRepository submissions;
    @Mock UserRepository users;
    @Mock AuthorizationService authorization;
    @InjectMocks IncidentService service;

    @Test
    void rejectsTrackFromAnotherEvent() {
        UUID eventId = UUID.randomUUID();
        UUID trackId = UUID.randomUUID();
        Event event = Event.builder().title("Selected").build();
        event.setId(eventId);
        Event otherEvent = Event.builder().title("Other").build();
        otherEvent.setId(UUID.randomUUID());
        Track track = Track.builder().event(otherEvent).name("Wrong").build();
        track.setId(trackId);

        CurrentUser principal = CurrentUser.builder().id(UUID.randomUUID())
                .roles(List.of("judge")).build();
        var authentication = new UsernamePasswordAuthenticationToken(principal, null);
        User reporter = User.builder().email("judge@example.com").fullName("Judge").passwordHash("x").build();
        reporter.setId(principal.getId());

        when(authorization.current(authentication)).thenReturn(principal);
        when(users.findById(principal.getId())).thenReturn(Optional.of(reporter));
        when(events.findById(eventId)).thenReturn(Optional.of(event));
        when(tracks.findById(trackId)).thenReturn(Optional.of(track));

        CreateIncidentRequest request = new CreateIncidentRequest(
                eventId, trackId, null, null, null, IncidentType.other,
                null, null, "Mismatch", "Wrong hierarchy");

        ApiException exception = assertThrows(ApiException.class,
                () -> service.create(request, authentication));
        assertEquals(400, exception.getStatus().value());
        verify(reports, never()).save(any());
    }
}
