package vn.edu.fpt.seal.modules.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.chat.dto.CreateTeamChatMessageRequest;
import vn.edu.fpt.seal.modules.chat.dto.TeamChatMessageResponse;
import vn.edu.fpt.seal.modules.chat.entity.TeamChatMessage;
import vn.edu.fpt.seal.modules.chat.repository.TeamChatMessageRepository;
import vn.edu.fpt.seal.modules.team.repository.TeamMemberRepository;
import vn.edu.fpt.seal.modules.team.repository.TeamRepository;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.security.AuthorizationService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamChatService {
    private final TeamChatMessageRepository repository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public List<TeamChatMessageResponse> list(UUID teamId, Authentication authentication) {
        if (!teamRepository.existsById(teamId)) {
            throw ApiException.notFound("Team not found");
        }
        var user = authorizationService.current(authentication);
        authorizationService.require(
                authorizationService.isCoordinator(user)
                        || teamMemberRepository.existsByTeamIdAndUserId(teamId, user.getId()),
                "Only team members and coordinators can read team chat");
        return repository.findTop100ByTeamIdOrderByCreatedAtAsc(teamId).stream().map(this::map).toList();
    }

    @Transactional
    public TeamChatMessageResponse create(CreateTeamChatMessageRequest request, UUID senderId) {
        var team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> ApiException.notFound("Team not found"));
        if (!teamMemberRepository.existsByTeamIdAndUserId(team.getId(), senderId)) {
            throw ApiException.forbidden("Only team members can post team chat messages");
        }
        var sender = userRepository.findById(senderId)
                .orElseThrow(() -> ApiException.notFound("Sender not found"));
        return map(repository.save(TeamChatMessage.builder()
                .team(team)
                .sender(sender)
                .message(request.message().trim())
                .build()));
    }

    private TeamChatMessageResponse map(TeamChatMessage message) {
        var sender = message.getSender();
        return new TeamChatMessageResponse(
                message.getId(), message.getTeam().getId(), sender.getId(),
                sender.getFullName(), sender.getEmail(), message.getMessage(), message.getCreatedAt());
    }
}
