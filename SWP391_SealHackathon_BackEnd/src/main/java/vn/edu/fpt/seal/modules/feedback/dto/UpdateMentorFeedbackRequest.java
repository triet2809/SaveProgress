package vn.edu.fpt.seal.modules.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMentorFeedbackRequest(@NotBlank @Size(max = 10000) String content) {
}
