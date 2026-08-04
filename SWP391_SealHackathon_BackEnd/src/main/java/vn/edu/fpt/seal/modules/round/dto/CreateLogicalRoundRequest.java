package vn.edu.fpt.seal.modules.round.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateLogicalRoundRequest(
        @NotEmpty List<@NotNull UUID> trackIds,
        @NotBlank @Size(max = 255) String name,
        @Min(1) Integer sequenceNumber,
        @NotNull @Future LocalDateTime submissionDeadline,
        @NotNull @Min(1) @Max(500) Integer topNToPromote,
        Boolean finalRound,
        @Min(1) Integer defaultTopNToPromote) {
    public CreateLogicalRoundRequest(List<UUID> trackIds, String name, Integer sequenceNumber,
                                     LocalDateTime submissionDeadline, Integer topNToPromote) {
        this(trackIds, name, sequenceNumber, submissionDeadline, topNToPromote, false, topNToPromote);
    }
}
