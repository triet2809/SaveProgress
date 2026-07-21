package vn.edu.fpt.seal.modules.event.dto;

import lombok.Builder;
import vn.edu.fpt.seal.common.enums.EventStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record EventResponse(
        UUID id,
        String title,
        String description,
        EventStatus status,
        String term,
        String prizePool,
        LocalDateTime registrationStart,
        LocalDateTime registrationEnd,
        LocalDateTime eventStart,
        LocalDateTime eventEnd,
        Integer roundsCount,
        Integer tracksCount,
        Long participantsCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
