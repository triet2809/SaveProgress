package vn.edu.fpt.seal.modules.round.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateLogicalRoundRequest(
        @Size(max = 255) String name,
        @Min(1) Integer sequenceNumber,
        Boolean finalRound,
        @Min(1) Integer defaultTopNToPromote) {
}
