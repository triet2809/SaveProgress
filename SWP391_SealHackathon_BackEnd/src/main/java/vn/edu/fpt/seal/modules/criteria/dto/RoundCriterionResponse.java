package vn.edu.fpt.seal.modules.criteria.dto;

import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record RoundCriterionResponse(UUID id, UUID roundId, UUID templateId, String name, BigDecimal weight, String description, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {}
