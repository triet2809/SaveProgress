package vn.edu.fpt.seal.modules.feedback.dto; import jakarta.validation.constraints.*;
public record UpdateMentorFeedbackRequest(@NotBlank @Size(max=10000) String content) {}
