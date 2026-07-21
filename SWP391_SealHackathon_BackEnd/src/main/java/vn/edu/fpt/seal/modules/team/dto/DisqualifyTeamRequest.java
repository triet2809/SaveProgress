package vn.edu.fpt.seal.modules.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DisqualifyTeamRequest(
        @NotBlank @Size(max = 10000) String reason
) {
}
