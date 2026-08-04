package vn.edu.fpt.seal.modules.track.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateTrackRequest(
        @NotNull UUID eventId,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 10000) String description,
        @Min(1) Integer maxTeams
) {
}
