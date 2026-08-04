package vn.edu.fpt.seal.modules.university.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record UniversityResponse(UUID id, String name, String shortName, String country, LocalDateTime createdAt,
                                 LocalDateTime updatedAt) {
}
