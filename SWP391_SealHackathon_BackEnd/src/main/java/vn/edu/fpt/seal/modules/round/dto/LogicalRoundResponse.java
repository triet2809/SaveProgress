package vn.edu.fpt.seal.modules.round.dto;

import java.util.List;
import java.util.UUID;

public record LogicalRoundResponse(
        UUID logicalRoundId,
        UUID eventId,
        String name,
        Integer sequenceNumber,
        boolean finalRound,
        Integer defaultTopNToPromote,
        String lifecycleState,
        List<RoundResponse> trackRounds) {
}
