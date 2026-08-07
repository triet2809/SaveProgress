package vn.edu.fpt.seal.modules.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateEventRequest(
        @NotBlank(message = "Event title must not be blank")
        @Size(max = 255, message = "Event title must not exceed 255 characters")
        String title,

        @Size(max = 10000, message = "Description must not exceed 10000 characters")
        String description,

        @Size(max = 255, message = "Term must not exceed 255 characters")
        String term,

        @Size(max = 255, message = "Prize pool must not exceed 255 characters")
        String prizePool,

        @NotNull(message = "Registration start date is required")
        LocalDateTime registrationStart,

        @NotNull(message = "Registration end date is required")
        LocalDateTime registrationEnd,

        @NotNull(message = "Event start date is required")
        LocalDateTime eventStart,

        @NotNull(message = "Event end date is required")
        LocalDateTime eventEnd
) {
}
