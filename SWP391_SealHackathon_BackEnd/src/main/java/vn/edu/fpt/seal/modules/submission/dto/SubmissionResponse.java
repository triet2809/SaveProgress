package vn.edu.fpt.seal.modules.submission.dto;

import lombok.Builder;
import vn.edu.fpt.seal.modules.recognition.dto.RecognitionDtos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record SubmissionResponse(
        UUID id,
        UUID roundId,
        UUID teamId,
        UUID trackId,
        String teamName,
        String repoUrl,
        String demoUrl,
        String slideUrl,
        String reportUrl,
        String apiMetadata,
        String projectName,
        String version,
        String reviewStatus,
        String status,
        LocalDateTime submittedAt,
        LocalDateTime updatedAt,
        List<RecognitionDtos.Summary> recognitions
) {
}
