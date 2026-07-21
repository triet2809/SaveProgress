package vn.edu.fpt.seal.modules.team.dto;

import jakarta.validation.constraints.Size;

public record UpdateTeamRequest(
        @Size(max = 255) String name
) {
}
