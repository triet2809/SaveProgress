package vn.edu.fpt.seal.modules.team;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import vn.edu.fpt.seal.common.enums.*;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.audit.repository.AuditLogRepository;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.event.repository.EventRepository;
import vn.edu.fpt.seal.modules.recognition.service.TeamRecognitionService;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.team.repository.TeamMemberRepository;
import vn.edu.fpt.seal.modules.team.repository.TeamRepository;
import vn.edu.fpt.seal.modules.team.service.TeamService;
import vn.edu.fpt.seal.modules.teamprofile.entity.TeamProfile;
import vn.edu.fpt.seal.modules.teamprofile.repository.TeamProfileRepository;
import vn.edu.fpt.seal.modules.timeline.service.TimelineService;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.track.repository.TrackRepository;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.security.CurrentUser;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceReactivationTest {
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
    private Team team;
    private Event event;
    private UsernamePasswordAuthenticationToken coordinator;

    @BeforeEach
    void setUp() {
        service = new TeamService(teams, members, tracks, users, audits, events, profiles, recognitions, timeline);
        event = Event.builder().title("Event").status(EventStatus.published).build();
        event.setId(UUID.randomUUID());
        Track track = Track.builder().event(event).name("Track").build();
        track.setId(UUID.randomUUID());
        TeamProfile profile = TeamProfile.builder().canonicalName("Team").status(TeamProfileStatus.active).build();
        profile.setId(UUID.randomUUID());
        team = Team.builder().teamProfile(profile).event(event).track(track).name("Team")
                .status(TeamStatus.disqualified).disqualifiedReason("Administrative correction").build();
        team.setId(UUID.randomUUID());
        CurrentUser actor = CurrentUser.builder().id(UUID.randomUUID()).email("ec@example.test")
                .roles(List.of("coordinator")).build();
        coordinator = new UsernamePasswordAuthenticationToken(actor, null,
                List.of(new SimpleGrantedAuthority("ROLE_COORDINATOR")));
        lenient().when(teams.findWithTrackById(team.getId())).thenReturn(Optional.of(team));
        lenient().when(teams.saveAndFlush(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(members.findByTeamIdOrderByRoleAscJoinedAtAsc(team.getId())).thenReturn(List.of());
        lenient().when(recognitions.activeByTeamIds(anyList())).thenReturn(Map.of());
    }

    @Test
    void coordinatorReactivatesEligibleDisqualifiedTeamAndPersistsIt() {
        var response = service.reactivate(team.getId(), coordinator);

        assertEquals(TeamStatus.active, team.getStatus());
        assertNull(team.getDisqualifiedReason());
        assertEquals(TeamStatus.active, response.status());
        verify(teams).saveAndFlush(team);
        verify(audits).save(any());
        verify(timeline).record(eq(event), eq(team.getId()), isNull(), eq(team.getTrack().getId()),
                eq("TEAM_REACTIVATED"), any(), anyString(), anyString(), eq("TEAM"), eq(team.getId()), anyString());
    }

    @Test
    void alreadyActiveTeamIsRejectedWithoutPersistence() {
        team.setStatus(TeamStatus.active);

        assertEquals(409, assertThrows(ApiException.class,
                () -> service.reactivate(team.getId(), coordinator)).getStatus().value());
        verify(teams, never()).saveAndFlush(any());
        verifyNoInteractions(timeline);
    }

    @Test
    void closedEventBlocksReactivation() {
        event.setStatus(EventStatus.ongoing);

        assertEquals(400, assertThrows(ApiException.class,
                () -> service.reactivate(team.getId(), coordinator)).getStatus().value());
        verify(teams, never()).saveAndFlush(any());
        verifyNoInteractions(timeline);
    }

    @Test
    void inactiveProfileBlocksReactivation() {
        team.getTeamProfile().setStatus(TeamProfileStatus.dissolved);

        assertEquals(400, assertThrows(ApiException.class,
                () -> service.reactivate(team.getId(), coordinator)).getStatus().value());
        verify(teams, never()).saveAndFlush(any());
        verifyNoInteractions(timeline);
    }
}
