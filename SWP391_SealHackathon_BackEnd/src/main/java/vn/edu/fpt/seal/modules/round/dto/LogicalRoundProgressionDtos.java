package vn.edu.fpt.seal.modules.round.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class LogicalRoundProgressionDtos {
    private LogicalRoundProgressionDtos() {}

    public record PromotedTeam(UUID teamId, String teamName, UUID sourceLogicalRoundId,
                               UUID targetLogicalRoundId, UUID assignedRoundId) {}

    public record Assignment(@NotNull UUID roundId, @NotEmpty Set<@NotNull UUID> teamIds) {}

    public record ManualAssignmentRequest(@NotEmpty List<@Valid Assignment> assignments) {}

    public record BalanceRequest(Long seed) {}

    public record AssignmentPlan(UUID roundId, UUID trackId, List<UUID> teamIds) {}

    public record BalancePreview(UUID logicalRoundId, long seed,
                                 List<AssignmentPlan> assignments,
                                 Map<UUID, Long> projectedCounts) {}
}
