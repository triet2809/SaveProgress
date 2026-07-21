package vn.edu.fpt.seal.modules.event.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateEventRequest(
        @Size(max = 255) String title,
        @Size(max = 10000) String description,
        @Size(max = 255) String term,
        @Size(max = 255) String prizePool,
        LocalDateTime registrationStart,
        LocalDateTime registrationEnd,
        LocalDateTime eventStart,
        LocalDateTime eventEnd
) {
}
