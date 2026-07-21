package vn.edu.fpt.seal.modules.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateTeamRequest(
        @NotNull UUID trackId,
        @NotBlank @Size(max = 255) String name,
        UUID leaderUserId,
        List<UUID> memberUserIds,
        // Optional member emails (self-service create-team form). Each must be an
        // existing approved user. Combined with leader, total must stay within 1-5.
        List<@Size(max = 255) String> memberEmails
) {
}
