package vn.edu.fpt.seal.modules.casework.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record CreateCaseRequest(@NotNull UUID eventId, UUID roundId, UUID trackId, UUID teamId, UUID submissionId,
                                @NotBlank String category, @NotBlank @Size(max=255) String subject,
                                @NotBlank @Size(max=10000) String description) {}
