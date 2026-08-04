package vn.edu.fpt.seal.modules.score.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record ScoreResponse(UUID id, UUID submissionId, UUID judgeId, String judgeEmail, UUID criterionId,
                            String criterionName, BigDecimal score, BigDecimal weightedScore, String comment,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
}
