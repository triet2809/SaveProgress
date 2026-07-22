package vn.edu.fpt.seal.modules.criteria.dto;

import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** DTO trả về một mẫu tiêu chí chấm điểm. */
@Builder
public record CriteriaTemplateResponse(UUID id, String name, String description, BigDecimal defaultWeight, LocalDateTime createdAt, LocalDateTime updatedAt) {}
