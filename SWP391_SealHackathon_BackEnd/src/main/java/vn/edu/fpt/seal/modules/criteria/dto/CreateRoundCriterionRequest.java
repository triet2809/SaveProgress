package vn.edu.fpt.seal.modules.criteria.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateRoundCriterionRequest(
        @NotNull UUID roundId,
        UUID templateId,
        @NotBlank @Size(max = 255) String name,
        @NotNull @DecimalMin("0.00") BigDecimal weight,
        @Size(max = 10000) String description,
        @Size(max = 50) String status
) {}
