package vn.edu.fpt.seal.modules.track.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;

public record UpdateTrackRequest(
        @Size(max = 255) String name,
        @Size(max = 10000) String description,
        @Min(1) Integer maxTeams
) {
}
