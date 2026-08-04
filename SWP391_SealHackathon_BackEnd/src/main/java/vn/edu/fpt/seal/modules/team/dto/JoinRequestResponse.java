package vn.edu.fpt.seal.modules.team.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record JoinRequestResponse(
        UUID id,
        UUID teamId,
        String teamName,
        UUID userId,
        String userEmail,
        String userFullName,
        String status,
        String message,
        LocalDateTime createdAt,
        LocalDateTime respondedAt
) {
}
