package vn.edu.fpt.seal.modules.round.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateRoundRequest(
        @Size(max = 255) String name,
        @Min(1) Integer sequenceNumber,
        @Future LocalDateTime submissionDeadline,
        @Min(1) Integer topNToPromote
) {
}
