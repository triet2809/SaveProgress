package vn.edu.fpt.seal.modules.event.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;
import vn.edu.fpt.seal.modules.seeding.dto.SeedingDtos;

/**
 * Result of the one-shot competition setup: what tracks/rounds were generated and
 * how the registered teams were distributed.
 */
@Builder
public record SetupCompetitionResponse(
        UUID eventId,
        int totalTeams,
        int trackCount,
        int roundsPerTrack,
        SeedingDtos.ReviewSummary seedReview,
        List<String> warnings,
        List<TrackPlan> tracks
) {
    @Builder
    public record TrackPlan(
            UUID trackId,
            String name,
            int teamCount,
            List<RoundPlan> rounds
    ) {
    }

    @Builder
    public record RoundPlan(
            UUID roundId,
            String name,
            int sequenceNumber,
            int topNToPromote,
            int seededParticipants
    ) {
    }
}
