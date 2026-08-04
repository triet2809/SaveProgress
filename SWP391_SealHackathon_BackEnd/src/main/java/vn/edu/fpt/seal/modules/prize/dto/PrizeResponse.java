package vn.edu.fpt.seal.modules.prize.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record PrizeResponse(UUID id, UUID eventId, UUID trackId, String trackName, UUID teamId, String teamName,
                            String name, BigDecimal prizeAmount, String description, LocalDateTime awardedAt,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
}
