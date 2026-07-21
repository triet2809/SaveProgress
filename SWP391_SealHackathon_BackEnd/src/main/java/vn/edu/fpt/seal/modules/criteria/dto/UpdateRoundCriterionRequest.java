package vn.edu.fpt.seal.modules.criteria.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record UpdateRoundCriterionRequest(
        @Size(max = 255) String name,
        @DecimalMin("0.00") BigDecimal weight,
        @Size(max = 10000) String description,
        @Size(max = 50) String status
) {}
