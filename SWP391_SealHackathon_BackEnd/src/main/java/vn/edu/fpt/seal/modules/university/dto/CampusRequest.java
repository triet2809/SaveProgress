package vn.edu.fpt.seal.modules.university.dto;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record CampusRequest(@NotNull UUID universityId, @NotBlank @Size(max = 255) String name,
                            @Size(max = 10000) String address, @Size(max = 100) String city) {
}
