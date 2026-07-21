package vn.edu.fpt.seal.modules.team;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import vn.edu.fpt.seal.common.enums.AccountStatus;
import vn.edu.fpt.seal.common.enums.EventStatus;
import vn.edu.fpt.seal.common.enums.TeamMemberRole;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.audit.repository.AuditLogRepository;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.event.repository.EventRepository;
import vn.edu.fpt.seal.modules.recognition.service.TeamRecognitionService;
import vn.edu.fpt.seal.modules.team.dto.CreateTeamRequest;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.team.entity.TeamMember;
import vn.edu.fpt.seal.modules.team.repository.TeamMemberRepository;
import vn.edu.fpt.seal.modules.team.repository.TeamRepository;
import vn.edu.fpt.seal.modules.team.service.TeamService;
import vn.edu.fpt.seal.modules.teamprofile.entity.TeamProfile;
import vn.edu.fpt.seal.modules.teamprofile.repository.TeamProfileRepository;
import vn.edu.fpt.seal.modules.timeline.service.TimelineService;
import vn.edu.fpt.seal.modules.track.repository.TrackRepository;
import vn.edu.fpt.seal.modules.user.entity.User;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.security.CurrentUser;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceCreationTest {
    @Mock TeamRepository teams;
    @Mock TeamMemberRepository members;
    @Mock TrackRepository tracks;
    @Mock UserRepository users;
    @Mock AuditLogRepository audits;
    @Mock EventRepository events;
    @Mock TeamProfileRepository profiles;
    @Mock TeamRecognitionService recognitions;
    @Mock TimelineService timeline;

    private TeamService service;
    private Event event;
    private User user;
    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        service = new TeamService(teams, members, tracks, users, audits, events, profiles, recognitions, timeline);
        event = Event.builder().title("Open event").status(EventStatus.published).build();
        event.setId(UUID.randomUUID());
        user = User.builder().email("student@example.test").fullName("Student")
                .status(AccountStatus.approved).build();
        user.setId(UUID.randomUUID());
        CurrentUser current = CurrentUser.builder().id(user.getId()).email(user.getEmail())
                .roles(List.of("team_member")).build();
        authentication = new UsernamePasswordAuthenticationToken(current, null,
                List.of(new SimpleGrantedAuthority("ROLE_TEAM_MEMBER")));

        lenient().when(events.findById(event.getId())).thenReturn(Optional.of(event));
        lenient().when(users.findById(user.getId())).thenReturn(Optional.of(user));
        lenient().when(profiles.save(any(TeamProfile.class))).thenAnswer(invocation -> {
            TeamProfile profile = invocation.getArgument(0);
            profile.setId(UUID.randomUUID());
            return profile;
        });
        lenient().when(teams.save(any(Team.class))).thenAnswer(invocation -> {
            Team team = invocation.getArgument(0);
            team.setId(UUID.randomUUID());
            return team;
        });
        lenient().when(members.save(any(TeamMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(recognitions.activeByTeamIds(anyList())).thenReturn(Map.of());
        lenient().when(members.findByTeamIdOrderByRoleAscJoinedAtAsc(any())).thenReturn(List.of());
    }

    @Test
    void approvedParticipantCreatesUnassignedTeamForSelectedEvent() {
        var response = service.create(new CreateTeamRequest(event.getId(), "Alpha", null, List.of(), List.of()),
                authentication);

        ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);
        verify(teams).save(teamCaptor.capture());
        Team created = teamCaptor.getValue();
        assertSame(event, created.getEvent());
        assertNull(created.getTrack());
        assertEquals(event.getId(), response.eventId());
        assertNull(response.trackId());
        verify(members).save(argThat(member -> member.getUser() == user
                && member.getRole() == TeamMemberRole.leader));
        verify(timeline).record(eq(event), eq(created.getId()), isNull(), isNull(), eq("TEAM_CREATED"),
                any(), anyString(), anyString(), eq("TEAM"), eq(created.getId()), anyString());
        verifyNoInteractions(tracks);
    }

    @Test
    void pendingParticipantCannotCreateTeam() {
        user.setStatus(AccountStatus.pending);

        ApiException error = assertThrows(ApiException.class, () -> service.create(
                new CreateTeamRequest(event.getId(), "Alpha", null, List.of(), List.of()), authentication));

        assertEquals(400, error.getStatus().value());
        assertEquals("Only approved users can join teams", error.getMessage());
    }

    @Test
    void closedEventCannotAcceptTeamCreation() {
        event.setStatus(EventStatus.ongoing);

        ApiException error = assertThrows(ApiException.class, () -> service.create(
                new CreateTeamRequest(event.getId(), "Alpha", null, List.of(), List.of()), authentication));

        assertEquals(400, error.getStatus().value());
        verify(teams, never()).save(any());
        verifyNoInteractions(timeline);
    }
}
