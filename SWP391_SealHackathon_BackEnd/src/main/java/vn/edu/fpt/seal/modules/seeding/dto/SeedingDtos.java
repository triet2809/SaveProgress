package vn.edu.fpt.seal.modules.seeding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import vn.edu.fpt.seal.modules.recognition.dto.RecognitionDtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class SeedingDtos {
    private SeedingDtos() {
    }

    public record FinalizedFinish(UUID finishId, UUID teamId, UUID teamProfileId,
                                  UUID trackId, UUID finalRoundId, UUID resultVersionId,
                                  int finalRank, BigDecimal finalScore, String completionStatus) {
    }

    public record FinalizationResponse(UUID eventId, int createdCount, int existingCount,
                                       List<FinalizedFinish> finishes) {
    }

    public record RosterMember(UUID userId, String fullName) {
    }

    public record ContinuityEvidence(UUID historicalFinishId, UUID eventId, String eventName,
                                     UUID trackId, String trackName, int finalRank,
                                     UUID resultVersionId, LocalDateTime completedAt, int historicalRosterSize,
                                     int returningMemberCount, boolean qualifiedByContinuity,
                                     List<RosterMember> matchingMembers) {
    }

    public record Assignment(UUID id, UUID teamId, UUID trackId, Integer seedNumber,
                             String seedTier, UUID candidateSourceFinishId, int continuityCount,
                             String status, String rationale, LocalDateTime assignedAt) {
    }

    public record Candidate(UUID teamId, String teamName, UUID teamProfileId, String profileName,
                            UUID trackId, String trackName, List<RosterMember> currentRoster,
                            List<ContinuityEvidence> supportingFinishes, int bestHistoricalRank,
                            UUID latestTopFiveFinishId, int topFiveFinishCount, String suggestedSeedTier,
                            String recommendationFormula, Assignment currentDecision,
                            List<RecognitionDtos.Summary> recognitions,
                            String qualificationReason) {
    }

    public record ReviewSummary(int eligibleCandidates, int confirmed, int rejected,
                                int overridden, int unreviewed, String warning) {
    }

    public record CandidateResponse(UUID eventId, UUID trackId, ReviewSummary summary,
                                    List<Candidate> candidates) {
    }

    public record SeedDecisionRequest(
            @NotBlank @Pattern(regexp = "confirmed|rejected|overridden") String status,
            @Positive Integer seedNumber,
            @Size(max = 20) String seedTier,
            UUID candidateSourceFinishId,
            @Size(max = 4000) String rationale) {
    }
}
