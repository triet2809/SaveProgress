package vn.edu.fpt.seal.modules.criteria.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO đầu vào tạo mẫu tiêu chí: tên bắt buộc, mô tả tuỳ chọn, trọng số mặc định >= 0.
 */
public record CreateCriteriaTemplateRequest(@NotBlank @Size(max = 255) String name,
                                            @Size(max = 10000) String description,
                                            @NotNull @DecimalMin("0.00") BigDecimal defaultWeight) {
}
