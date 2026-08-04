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

/**
 * Service nghiệp vụ chat nội bộ của đội.
 * Quản lý đọc/gửi tin nhắn kèm kiểm tra quyền truy cập theo thành viên đội/coordinator.
 */
@Service
@RequiredArgsConstructor
public class TeamChatService {
    private final TeamChatMessageRepository repository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;

    /**
     * Lấy tối đa 100 tin nhắn của một đội.
     * Chỉ thành viên đội hoặc coordinator mới được đọc.
     *
     * @throws ApiException nếu đội không tồn tại hoặc ngoài phạm vi truy cập
     */
    @Transactional(readOnly = true)
    public List<TeamChatMessageResponse> list(UUID teamId, Authentication authentication) {
        if (!teamRepository.existsById(teamId)) {
            throw ApiException.notFound("Team not found");
        }
        var user = authorizationService.current(authentication);
        // Chặn truy cập: phải là coordinator hoặc thành viên của đội
        authorizationService.require(
                authorizationService.isCoordinator(user)
                        || teamMemberRepository.existsByTeamIdAndUserId(teamId, user.getId()),
                "Only team members and coordinators can read team chat");
        return repository.findTop100ByTeamIdOrderByCreatedAtAsc(teamId).stream().map(this::map).toList();
    }

    /**
     * Gửi tin nhắn mới vào khung chat của đội.
     * Chỉ thành viên đội được phép gửi; nội dung được trim trước khi lưu.
     *
     * @param senderId id người gửi (lấy từ principal)
     * @throws ApiException nếu đội/người gửi không tồn tại hoặc người gửi không phải thành viên
     */
    @Transactional
    public TeamChatMessageResponse create(CreateTeamChatMessageRequest request, UUID senderId) {
        var team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> ApiException.notFound("Team not found"));
        // Chỉ thành viên đội mới được đăng tin
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

    /**
     * Ánh xạ thực thể TeamChatMessage sang DTO Response kèm thông tin người gửi.
     */
    private TeamChatMessageResponse map(TeamChatMessage message) {
        var sender = message.getSender();
        return new TeamChatMessageResponse(
                message.getId(), message.getTeam().getId(), sender.getId(),
                sender.getFullName(), sender.getEmail(), message.getMessage(), message.getCreatedAt());
    }
}
