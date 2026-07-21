package vn.edu.fpt.seal.modules.criteria.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record UpdateCriteriaTemplateRequest(@Size(max = 255) String name, @Size(max = 10000) String description, @DecimalMin("0.00") BigDecimal defaultWeight) {}
