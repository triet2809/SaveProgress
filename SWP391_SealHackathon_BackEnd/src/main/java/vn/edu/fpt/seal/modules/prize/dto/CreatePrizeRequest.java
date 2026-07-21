package vn.edu.fpt.seal.modules.prize.dto; import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.LocalDateTime; import java.util.UUID;
public record CreatePrizeRequest(@NotNull UUID eventId, UUID trackId, UUID teamId, @NotBlank @Size(max=255) String name, @DecimalMin("0.00") BigDecimal prizeAmount, @Size(max=10000) String description, LocalDateTime awardedAt) {}
