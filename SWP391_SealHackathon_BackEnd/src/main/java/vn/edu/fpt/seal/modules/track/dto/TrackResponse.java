package vn.edu.fpt.seal.modules.track.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record TrackResponse(
        UUID id,
        UUID eventId,
        String name,
        String description,
        Integer maxTeams,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
