package vn.edu.fpt.seal.modules.ranking.dto;

import lombok.Builder;
import vn.edu.fpt.seal.common.enums.PromotionStatus;
import vn.edu.fpt.seal.modules.recognition.dto.RecognitionDtos;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record RoundRankingResponse(UUID id, UUID roundId, UUID trackId, String trackName, UUID teamId, String teamName,
                                   BigDecimal totalScore, Integer rank, PromotionStatus status, LocalDateTime resultPublishedAt,
                                   UUID tieBreakerCriterionId, BigDecimal tieBreakerScore, String tieBreakerReason,
                                   LocalDateTime calculatedAt, LocalDateTime updatedAt,
                                   List<RecognitionDtos.Summary> recognitions) {}
