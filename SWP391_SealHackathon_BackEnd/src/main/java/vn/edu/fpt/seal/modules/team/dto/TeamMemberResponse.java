package vn.edu.fpt.seal.modules.team.dto;

import lombok.Builder;
import vn.edu.fpt.seal.common.enums.TeamMemberRole;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record TeamMemberResponse(
        UUID id,
        UUID userId,
        String email,
        String fullName,
        TeamMemberRole role,
        LocalDateTime joinedAt
) {
}
