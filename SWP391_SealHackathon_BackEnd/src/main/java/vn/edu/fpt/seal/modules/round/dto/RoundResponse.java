package vn.edu.fpt.seal.modules.round.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record RoundResponse(
        UUID id,
        UUID logicalRoundId,
        UUID trackId,
        UUID eventId,
        String name,
        Integer sequenceNumber,
        LocalDateTime submissionDeadline,
        Integer topNToPromote,
        LocalDateTime resultPublishedAt,
        LocalDateTime appealDeadline,
        String lifecycleState,
        Long remainingSeconds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
