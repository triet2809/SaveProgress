package vn.edu.fpt.seal.modules.round.mapper;

import vn.edu.fpt.seal.modules.round.dto.RoundResponse;
import vn.edu.fpt.seal.modules.round.entity.Round;

public final class RoundMapper {

    private RoundMapper() {
    }

    public static RoundResponse toResponse(Round r) {
        return toResponse(r, null);
    }

    public static RoundResponse toResponse(Round r, Long remainingSeconds) {
        var definition = r.getLogicalRound();
        return RoundResponse.builder()
                .id(r.getId())
                .logicalRoundId(definition == null ? null : definition.getId())
                .trackId(r.getTrack().getId())
                .eventId(r.getTrack().getEvent().getId())
                .name(definition == null ? r.getName() : definition.getName())
                .sequenceNumber(definition == null ? r.getSequenceNumber() : definition.getSequenceNumber())
                .submissionDeadline(r.getSubmissionDeadline())
                .topNToPromote(r.getTopNToPromote())
                .resultPublishedAt(r.getResultPublishedAt())
                .appealDeadline(r.getAppealDeadline())
                .lifecycleState(definition == null ? r.getLifecycleState().name() : definition.getLifecycleState().name())
                .remainingSeconds(remainingSeconds)
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
