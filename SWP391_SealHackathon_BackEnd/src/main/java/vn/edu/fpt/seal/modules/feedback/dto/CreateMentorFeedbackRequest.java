package vn.edu.fpt.seal.modules.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateMentorFeedbackRequest(@NotNull UUID trackMentorId, @NotNull UUID teamId, UUID roundId,
                                          @NotBlank @Size(max = 10000) String content) {
}
