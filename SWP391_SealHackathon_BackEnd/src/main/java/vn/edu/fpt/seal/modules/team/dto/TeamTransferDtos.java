package vn.edu.fpt.seal.modules.team.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public final class TeamTransferDtos {
    private TeamTransferDtos() {}

    public record BulkTransferRequest(@NotEmpty List<@NotNull UUID> teamIds, @NotNull UUID targetTrackId) {}
    public record BalanceRequest(@NotEmpty List<@NotNull UUID> targetTrackIds, Long randomSeed) {}
    public record TeamMove(UUID teamId, String teamName, UUID currentTrackId, UUID proposedTrackId) {}
    public record ExcludedTeam(UUID teamId, String teamName, String reason) {}
    public record TrackCount(UUID trackId, String trackName, long beforeCount, long afterCount) {}
    public record TransferResult(List<TeamMove> moves, List<ExcludedTeam> excluded, List<TrackCount> counts) {}
}
