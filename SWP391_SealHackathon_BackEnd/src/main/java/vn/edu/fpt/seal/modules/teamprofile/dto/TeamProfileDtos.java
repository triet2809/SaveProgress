package vn.edu.fpt.seal.modules.teamprofile.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.edu.fpt.seal.common.enums.TeamMemberRole;
import vn.edu.fpt.seal.modules.recognition.dto.RecognitionDtos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class TeamProfileDtos {
    private TeamProfileDtos() {
    }

    public record HistoricalMember(
            UUID userId,
            String fullName,
            TeamMemberRole historicalRole,
            LocalDateTime joinedAt
    ) {
    }

    public record HistoricalRegistration(
            UUID historicalTeamId,
            UUID eventId,
            String eventName,
            String eventStatus,
            UUID trackId,
            String trackName,
            TeamMemberRole currentUserHistoricalRole,
            List<HistoricalMember> historicalRoster
    ) {
    }

    public record ProfileSummary(
            UUID teamProfileId,
            String canonicalName,
            String logoUrl,
            List<RecognitionDtos.Summary> recognitions,
            boolean eligibleToInitiate,
            boolean alreadyRegisteredForTargetEvent,
            List<HistoricalRegistration> previousRegistrations
    ) {
    }

    public record ReactivationRequest(
            @NotNull UUID sourceTeamId,
            @NotNull UUID targetEventId,
            @NotNull UUID targetTrackId,
            @NotNull @Size(min = 1, max = 5) Set<UUID> returningMemberIds,
            @NotNull UUID leaderId
    ) {
    }

    public record MemberConflict(UUID userId, String fullName, UUID conflictingTeamId, String reason) {
    }

    public record ProposedMember(UUID userId, String fullName, TeamMemberRole role) {
    }

    public record PreviewResponse(
            boolean eligible,
            int returningMemberCount,
            List<MemberConflict> memberConflicts,
            List<String> missingRequirements,
            List<HistoricalMember> historicalRoster,
            List<ProposedMember> proposedNewRoster,
            List<String> warnings
    ) {
    }
}
