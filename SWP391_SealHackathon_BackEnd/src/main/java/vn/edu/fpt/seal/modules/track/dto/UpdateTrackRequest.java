package vn.edu.fpt.seal.modules.track.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateTrackRequest(
        @Size(max = 255) String name,
        @Size(max = 10000) String description,
        @Min(1) Integer maxTeams
) {
}
