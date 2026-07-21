package vn.edu.fpt.seal.modules.criteria.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateCriteriaTemplateRequest(@NotBlank @Size(max = 255) String name, @Size(max = 10000) String description, @NotNull @DecimalMin("0.00") BigDecimal defaultWeight) {}
