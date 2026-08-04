package vn.edu.fpt.seal.modules.criteria.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO đầu vào tạo tiêu chí cho vòng thi.
 * roundId bắt buộc; templateId tuỳ chọn (nếu tạo từ mẫu); trọng số > 0.
 */
public record CreateRoundCriterionRequest(
        @NotNull UUID roundId,
        UUID templateId,
        @NotBlank @Size(max = 255) String name,
        @NotNull @DecimalMin(value = "0.01", message = "weight must be greater than 0") BigDecimal weight,
        @Size(max = 10000) String description,
        @Size(max = 50) String status
) {
}
