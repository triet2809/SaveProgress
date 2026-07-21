package vn.edu.fpt.seal.modules.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.chat.repository.TeamChatMessageRepository;
import vn.edu.fpt.seal.modules.team.repository.TeamMemberRepository;
import vn.edu.fpt.seal.modules.team.repository.TeamRepository;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.security.AuthorizationService;
import vn.edu.fpt.seal.security.CurrentUser;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamChatServiceTest {
    @Mock TeamChatMessageRepository messages;
    @Mock TeamRepository teams;
    @Mock TeamMemberRepository members;
    @Mock UserRepository users;
    @Mock AuthorizationService authorization;
    TeamChatService service;
    UUID teamId;
    CurrentUser current;
    UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        service = new TeamChatService(messages, teams, members, users, authorization);
        teamId = UUID.randomUUID();
        current = CurrentUser.builder().id(UUID.randomUUID()).roles(List.of("team_member")).build();
        authentication = new UsernamePasswordAuthenticationToken(current, null);
        when(teams.existsById(teamId)).thenReturn(true);
        when(authorization.current(authentication)).thenReturn(current);
    }

    @Test
    void ownTeamMemberCanRead() {
        when(members.existsByTeamIdAndUserId(teamId, current.getId())).thenReturn(true);
        when(messages.findTop100ByTeamIdOrderByCreatedAtAsc(teamId)).thenReturn(List.of());
        assertDoesNotThrow(() -> service.list(teamId, authentication));
    }

    @Test
    void anotherTeamMemberIsDenied() {
        doThrow(ApiException.forbidden("denied")).when(authorization)
                .require(false, "Only team members and coordinators can read team chat");
        assertEquals(403, assertThrows(ApiException.class,
                () -> service.list(teamId, authentication)).getStatus().value());
    }

    @Test
    void coordinatorCanRead() {
        when(authorization.isCoordinator(current)).thenReturn(true);
        when(messages.findTop100ByTeamIdOrderByCreatedAtAsc(teamId)).thenReturn(List.of());
        assertDoesNotThrow(() -> service.list(teamId, authentication));
    }
}
