package vn.edu.fpt.seal.modules.criteria.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * DTO đầu vào cập nhật tiêu chí vòng thi.
 * Các trường đều tuỳ chọn; chỉ cập nhật trường khác null (partial update).
 */
public record UpdateRoundCriterionRequest(
        @Size(max = 255) String name,
        @DecimalMin(value = "0.01", message = "weight must be greater than 0") BigDecimal weight,
        @Size(max = 10000) String description,
        @Size(max = 50) String status
) {}
