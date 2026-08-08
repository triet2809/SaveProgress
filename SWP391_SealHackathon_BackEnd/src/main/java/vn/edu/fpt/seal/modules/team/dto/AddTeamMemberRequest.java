package vn.edu.fpt.seal.modules.team.dto;

import vn.edu.fpt.seal.common.enums.TeamMemberRole;

import java.util.UUID;

/**
 * Thêm thành viên vào team. Có thể chỉ định bằng {@code userId} (UUID user đã có)
 * hoặc bằng {@code email} của một user ĐÃ đăng ký + được duyệt. Bắt buộc có 1 trong 2.
 */
public record AddTeamMemberRequest(
        UUID userId,
        String email,
        TeamMemberRole role
) {
}
