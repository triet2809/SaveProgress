package vn.edu.fpt.seal.modules.feedback.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record MentorFeedbackResponse(UUID id, UUID trackMentorId, UUID mentorId, String mentorEmail, UUID trackId,
                                     String trackName, UUID teamId, String teamName, UUID roundId, String roundName,
                                     String content, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
