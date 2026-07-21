package vn.edu.fpt.seal.modules.round.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateRoundRequest(
        @NotNull UUID trackId,
        @NotBlank @Size(max = 255) String name,
        @Min(1) Integer sequenceNumber,
        @NotNull @Future LocalDateTime submissionDeadline,
        @NotNull @Min(1) Integer topNToPromote
) {
}
