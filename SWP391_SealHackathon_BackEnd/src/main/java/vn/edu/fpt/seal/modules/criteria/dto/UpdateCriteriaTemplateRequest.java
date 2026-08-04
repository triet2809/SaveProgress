package vn.edu.fpt.seal.modules.criteria.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO đầu vào cập nhật mẫu tiêu chí; các trường tuỳ chọn (partial update).
 */
public record UpdateCriteriaTemplateRequest(@Size(max = 255) String name, @Size(max = 10000) String description,
                                            @DecimalMin("0.00") BigDecimal defaultWeight) {
}
