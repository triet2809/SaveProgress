package vn.edu.fpt.seal.modules.university.dto;

import jakarta.validation.constraints.*;

public record UniversityRequest(@NotBlank @Size(max = 255) String name, @Size(max = 100) String shortName,
                                @Size(max = 100) String country) {
}
