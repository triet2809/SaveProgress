package vn.edu.fpt.seal.modules.team.dto;

import jakarta.validation.constraints.NotNull;
import vn.edu.fpt.seal.common.enums.TeamMemberRole;

import java.util.UUID;

public record AddTeamMemberRequest(
        @NotNull UUID userId,
        TeamMemberRole role
) {
}
