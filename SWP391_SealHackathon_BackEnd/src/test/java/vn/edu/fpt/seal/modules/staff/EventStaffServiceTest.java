package vn.edu.fpt.seal.modules.staff;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.fpt.seal.common.enums.AccountStatus;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.event.repository.EventRepository;
import vn.edu.fpt.seal.modules.judge.repository.*;
import vn.edu.fpt.seal.modules.mentor.repository.TrackMentorRepository;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;
import vn.edu.fpt.seal.modules.staff.dto.*;
import vn.edu.fpt.seal.modules.staff.service.EventStaffService;
import vn.edu.fpt.seal.modules.auth.service.AccountActivationService;
import vn.edu.fpt.seal.config.AppProperties;
import vn.edu.fpt.seal.modules.track.repository.TrackRepository;
import vn.edu.fpt.seal.modules.user.entity.*;
import vn.edu.fpt.seal.modules.user.repository.*;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventStaffServiceTest {
    @Mock EventRepository events; @Mock UserRepository users; @Mock RoleRepository roles;
    @Mock TrackRepository tracks; @Mock RoundRepository rounds; @Mock TrackMentorRepository mentors;
    @Mock TrackJudgeRepository trackJudges; @Mock RoundJudgeRepository roundJudges; @Mock PasswordEncoder encoder;
    @Mock AccountActivationService activation;

    @Test
    void listsOnlyAssignmentsFromSelectedEventAndDerivesEventRoles() {
        UUID eventId = UUID.randomUUID(), userId = UUID.randomUUID();
        Event event = Event.builder().title("Event A").build(); event.setId(eventId);
        User user = User.builder().email("judge@example.test").fullName("Judge").status(AccountStatus.approved)
                .roles(new HashSet<>(Set.of(Role.builder().name("judge").build()))).build();
        user.setId(userId);
        when(events.findById(eventId)).thenReturn(Optional.of(event));
        when(mentors.findAllByEventId(eventId)).thenReturn(List.of());
        when(trackJudges.findAllByEventId(eventId)).thenReturn(List.of());
        when(roundJudges.findAllByRoundTrackEventId(eventId)).thenReturn(List.of());
        assertTrue(new EventStaffService(events, users, roles, tracks, rounds, mentors, trackJudges, roundJudges, encoder)
                .list(eventId).isEmpty());
    }

    @Test
    void coordinatorCreatesApprovedMentorWithTemporaryPasswordChangeRequired() {
        UUID eventId = UUID.randomUUID(), trackId = UUID.randomUUID();
        Event event = Event.builder().title("Event").build(); event.setId(eventId);
        TrackStub track = track(eventId, trackId);
        Role mentor = Role.builder().name("mentor").build();
        when(events.findById(eventId)).thenReturn(Optional.of(event));
        when(users.findByEmail("new@example.test")).thenReturn(Optional.empty());
        when(encoder.encode(anyString())).thenReturn("hash");
        when(roles.findByName("mentor")).thenReturn(Optional.of(mentor));
        when(tracks.findById(trackId)).thenReturn(Optional.of(track.value));
        List<vn.edu.fpt.seal.modules.mentor.entity.TrackMentor> assigned = new ArrayList<>();
        when(mentors.findAllByEventId(eventId)).thenReturn(assigned);
        when(trackJudges.findAllByEventId(eventId)).thenReturn(new ArrayList<>());
        when(roundJudges.findAllByRoundTrackEventId(eventId)).thenReturn(new ArrayList<>());
        when(users.save(any(User.class))).thenAnswer(inv -> { User value = inv.getArgument(0); if (value.getId() == null) value.setId(UUID.randomUUID()); return value; });
        when(mentors.save(any())).thenAnswer(inv -> { assigned.add(inv.getArgument(0)); return inv.getArgument(0); });
        EventStaffService service = new EventStaffService(events, users, roles, tracks, rounds, mentors, trackJudges, roundJudges, encoder);
        EventStaffResponse response = service.invite(eventId, new InviteStaffRequest("New Mentor", "new@example.test",
                Set.of("mentor"), Set.of(trackId), Set.of(), Set.of(), null, null, "Temporary123"));
        assertNotNull(response);
        verify(encoder).encode(anyString());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users, atLeastOnce()).save(captor.capture());
        assertEquals(AccountStatus.approved, captor.getValue().getStatus());
        assertTrue(captor.getValue().isMustChangePassword());
        assertEquals("hash", captor.getValue().getPasswordHash());
        assertFalse(response.toString().contains("Temporary123"));
    }

    @Test
    void crossEventTrackIsRejected() {
        UUID eventId = UUID.randomUUID();
        Event event = Event.builder().title("Event").build(); event.setId(eventId);
        TrackStub other = track(UUID.randomUUID(), UUID.randomUUID());
        when(events.findById(eventId)).thenReturn(Optional.of(event));
        when(users.findByEmail("new@example.test")).thenReturn(Optional.empty());
        when(encoder.encode(anyString())).thenReturn("hash");
        when(roles.findByName("mentor")).thenReturn(Optional.of(Role.builder().name("mentor").build()));
        when(tracks.findById(other.value.getId())).thenReturn(Optional.of(other.value));
        when(users.save(any(User.class))).thenAnswer(inv -> { User value = inv.getArgument(0); if (value.getId() == null) value.setId(UUID.randomUUID()); return value; });
        EventStaffService service = new EventStaffService(events, users, roles, tracks, rounds, mentors, trackJudges, roundJudges, encoder);
        assertThrows(ApiException.class, () -> service.invite(eventId, new InviteStaffRequest("New", "new@example.test",
                Set.of("mentor"), Set.of(other.value.getId()), Set.of(), Set.of(), null, null, "Temporary123")));
    }

    @Test
    void coordinatorCreatesJudgeWithImmediateApproval() {
        UUID eventId = UUID.randomUUID(), trackId = UUID.randomUUID();
        Event event = Event.builder().title("Event").build(); event.setId(eventId);
        TrackStub track = track(eventId, trackId);
        List<vn.edu.fpt.seal.modules.judge.entity.TrackJudge> assigned = new ArrayList<>();
        when(events.findById(eventId)).thenReturn(Optional.of(event));
        when(users.findByEmail("judge@example.test")).thenReturn(Optional.empty());
        when(encoder.encode(anyString())).thenReturn("hash");
        when(roles.findByName("judge")).thenReturn(Optional.of(Role.builder().name("judge").build()));
        when(tracks.findById(trackId)).thenReturn(Optional.of(track.value));
        when(mentors.findAllByEventId(eventId)).thenReturn(new ArrayList<>());
        when(trackJudges.findAllByEventId(eventId)).thenReturn(assigned);
        when(roundJudges.findAllByRoundTrackEventId(eventId)).thenReturn(new ArrayList<>());
        when(users.save(any(User.class))).thenAnswer(inv -> { User value = inv.getArgument(0); if (value.getId() == null) value.setId(UUID.randomUUID()); return value; });
        when(trackJudges.save(any())).thenAnswer(inv -> { assigned.add(inv.getArgument(0)); return inv.getArgument(0); });

        EventStaffResponse response = new EventStaffService(events, users, roles, tracks, rounds, mentors, trackJudges, roundJudges, encoder)
                .invite(eventId, new InviteStaffRequest("Judge", "judge@example.test", Set.of("judge"),
                        Set.of(), Set.of(trackId), Set.of(), null, null, "Temporary123"));

        assertEquals(AccountStatus.approved, response.accountStatus());
        assertTrue(response.eventRoles().contains("judge"));
    }

    @Test
    void coordinatorCreatesCombinedJudgeAndMentorAccount() {
        UUID eventId = UUID.randomUUID(), trackId = UUID.randomUUID();
        Event event = Event.builder().title("Event").build(); event.setId(eventId);
        TrackStub track = track(eventId, trackId);
        Role judge = Role.builder().name("judge").build();
        Role mentor = Role.builder().name("mentor").build();
        List<vn.edu.fpt.seal.modules.mentor.entity.TrackMentor> mentorAssignments = new ArrayList<>();
        List<vn.edu.fpt.seal.modules.judge.entity.TrackJudge> judgeAssignments = new ArrayList<>();
        when(events.findById(eventId)).thenReturn(Optional.of(event));
        when(users.findByEmail("both@example.test")).thenReturn(Optional.empty());
        when(encoder.encode(anyString())).thenReturn("hash");
        when(roles.findByName("judge")).thenReturn(Optional.of(judge));
        when(roles.findByName("mentor")).thenReturn(Optional.of(mentor));
        when(tracks.findById(trackId)).thenReturn(Optional.of(track.value));
        when(mentors.findAllByEventId(eventId)).thenReturn(mentorAssignments);
        when(trackJudges.findAllByEventId(eventId)).thenReturn(judgeAssignments);
        when(roundJudges.findAllByRoundTrackEventId(eventId)).thenReturn(new ArrayList<>());
        when(users.save(any(User.class))).thenAnswer(inv -> { User value = inv.getArgument(0); if (value.getId() == null) value.setId(UUID.randomUUID()); return value; });
        when(mentors.save(any())).thenAnswer(inv -> { mentorAssignments.add(inv.getArgument(0)); return inv.getArgument(0); });
        when(trackJudges.save(any())).thenAnswer(inv -> { judgeAssignments.add(inv.getArgument(0)); return inv.getArgument(0); });

        EventStaffResponse response = new EventStaffService(events, users, roles, tracks, rounds, mentors, trackJudges, roundJudges, encoder)
                .invite(eventId, new InviteStaffRequest("Both", "both@example.test", Set.of("judge", "mentor"),
                        Set.of(trackId), Set.of(trackId), Set.of(), null, null, "Temporary123"));

        assertEquals(AccountStatus.approved, response.accountStatus());
        assertEquals(Set.of("judge", "mentor"), new HashSet<>(response.eventRoles()));
    }

    @Test
    void duplicateEmailCannotReceiveAnotherTemporaryPassword() {
        User existing = User.builder().email("existing@example.test").fullName("Existing")
                .status(AccountStatus.approved).roles(new HashSet<>()).build();
        existing.setId(UUID.randomUUID());
        when(events.findById(any())).thenReturn(Optional.of(Event.builder().title("Event").build()));
        when(users.findByEmail("existing@example.test")).thenReturn(Optional.of(existing));

        assertThrows(ApiException.class, () -> new EventStaffService(events, users, roles, tracks, rounds,
                mentors, trackJudges, roundJudges, encoder).invite(UUID.randomUUID(),
                new InviteStaffRequest("Existing", "existing@example.test", Set.of("judge"),
                        Set.of(), Set.of(), Set.of(), null, null, "Temporary123")));
        verify(encoder, never()).encode(anyString());
    }

    @Test
    void rejectedAccountCannotBeInvitedOrAssigned() {
        User rejected = User.builder().email("rejected@example.test").fullName("Rejected")
                .status(AccountStatus.rejected).roles(new HashSet<>()).build();
        rejected.setId(UUID.randomUUID());
        when(events.findById(any())).thenReturn(Optional.of(Event.builder().title("Event").build()));
        when(users.findByEmail("rejected@example.test")).thenReturn(Optional.of(rejected));

        assertThrows(ApiException.class, () -> new EventStaffService(events, users, roles, tracks, rounds,
                mentors, trackJudges, roundJudges, encoder).invite(UUID.randomUUID(),
                new InviteStaffRequest("Rejected", "rejected@example.test", Set.of("judge"),
                        Set.of(), Set.of(), Set.of(), null, null, null)));

        assertEquals(AccountStatus.rejected, rejected.getStatus());
        verify(users, never()).save(any());
        verifyNoInteractions(activation);
    }

    @Test
    void existingApprovedAccountCanBeAssignedWithoutTemporaryPassword() {
        UUID eventId = UUID.randomUUID(), trackId = UUID.randomUUID();
        Event event = Event.builder().title("Event").build(); event.setId(eventId);
        TrackStub track = track(eventId, trackId);
        User existing = User.builder().email("existing@example.test").fullName("Existing")
                .status(AccountStatus.approved).roles(new HashSet<>()).passwordHash("unchanged").build();
        existing.setId(UUID.randomUUID());
        List<vn.edu.fpt.seal.modules.judge.entity.TrackJudge> assigned = new ArrayList<>();
        when(events.findById(eventId)).thenReturn(Optional.of(event));
        when(users.findByEmail(existing.getEmail())).thenReturn(Optional.of(existing));
        when(roles.findByName("judge")).thenReturn(Optional.of(Role.builder().name("judge").build()));
        when(tracks.findById(trackId)).thenReturn(Optional.of(track.value));
        when(mentors.findAllByEventId(eventId)).thenReturn(new ArrayList<>());
        when(trackJudges.findAllByEventId(eventId)).thenReturn(assigned);
        when(roundJudges.findAllByRoundTrackEventId(eventId)).thenReturn(new ArrayList<>());
        when(trackJudges.save(any())).thenAnswer(inv -> { assigned.add(inv.getArgument(0)); return inv.getArgument(0); });

        EventStaffResponse response = new EventStaffService(events, users, roles, tracks, rounds,
                mentors, trackJudges, roundJudges, encoder).invite(eventId,
                new InviteStaffRequest("Existing", existing.getEmail(), Set.of("judge"),
                        Set.of(), Set.of(trackId), Set.of(), null, null, null));

        assertEquals(AccountStatus.approved, response.accountStatus());
        assertEquals("unchanged", existing.getPasswordHash());
        verifyNoInteractions(encoder);
    }

    @Test
    void existingPendingAccountCanBeAssignedWithoutTemporaryPassword() {
        UUID eventId = UUID.randomUUID(), trackId = UUID.randomUUID();
        Event event = Event.builder().title("Event").build(); event.setId(eventId);
        TrackStub track = track(eventId, trackId);
        User existing = User.builder().email("pending@example.test").fullName("Pending")
                .status(AccountStatus.pending).roles(new HashSet<>()).passwordHash("unchanged").build();
        existing.setId(UUID.randomUUID());
        List<vn.edu.fpt.seal.modules.judge.entity.TrackJudge> assigned = new ArrayList<>();
        when(events.findById(eventId)).thenReturn(Optional.of(event));
        when(users.findByEmail(existing.getEmail())).thenReturn(Optional.of(existing));
        when(roles.findByName("judge")).thenReturn(Optional.of(Role.builder().name("judge").build()));
        when(tracks.findById(trackId)).thenReturn(Optional.of(track.value));
        when(mentors.findAllByEventId(eventId)).thenReturn(new ArrayList<>());
        when(trackJudges.findAllByEventId(eventId)).thenReturn(assigned);
        when(roundJudges.findAllByRoundTrackEventId(eventId)).thenReturn(new ArrayList<>());
        when(trackJudges.save(any())).thenAnswer(inv -> { assigned.add(inv.getArgument(0)); return inv.getArgument(0); });
        when(activation.issue(existing)).thenReturn(new AccountActivationService.IssuedToken(
                "token", java.time.LocalDateTime.now().plusMinutes(30)));

        EventStaffResponse response = new EventStaffService(events, users, roles, tracks, rounds,
                mentors, trackJudges, roundJudges, encoder, activation, new AppProperties()).invite(eventId,
                new InviteStaffRequest("Pending", existing.getEmail(), Set.of("judge"),
                        Set.of(), Set.of(trackId), Set.of(), null, null, null));

        assertEquals(AccountStatus.pending, response.accountStatus());
        assertEquals("unchanged", existing.getPasswordHash());
        verify(activation).issue(existing);
        verifyNoInteractions(encoder);
    }

    private static TrackStub track(UUID eventId, UUID trackId) {
        Event event = Event.builder().title("E").build(); event.setId(eventId);
        var track = vn.edu.fpt.seal.modules.track.entity.Track.builder().event(event).name("Track").build(); track.setId(trackId);
        return new TrackStub(track);
    }
    private record TrackStub(vn.edu.fpt.seal.modules.track.entity.Track value) {}
}
