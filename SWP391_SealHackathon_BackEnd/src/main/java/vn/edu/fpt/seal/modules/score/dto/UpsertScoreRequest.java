package vn.edu.fpt.seal.modules.score.dto;

import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.util.UUID;
public record UpsertScoreRequest(@NotNull UUID submissionId, @NotNull UUID criterionId, @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal score, @Size(max=10000) String comment) {}
