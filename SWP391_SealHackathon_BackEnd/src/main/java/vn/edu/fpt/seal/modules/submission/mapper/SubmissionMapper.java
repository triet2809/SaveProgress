package vn.edu.fpt.seal.modules.submission.mapper;

import vn.edu.fpt.seal.modules.recognition.dto.RecognitionDtos;
import vn.edu.fpt.seal.modules.submission.dto.SubmissionResponse;
import vn.edu.fpt.seal.modules.submission.entity.Submission;

import java.util.List;

public final class SubmissionMapper {
    private SubmissionMapper() {
    }

    public static SubmissionResponse toResponse(Submission s) {
        return toResponse(s, List.of());
    }

    public static SubmissionResponse toResponse(
            Submission s, List<RecognitionDtos.Summary> recognitions) {
        return SubmissionResponse.builder()
                .id(s.getId())
                .roundId(s.getRound().getId())
                .teamId(s.getTeam().getId())
                .trackId(s.getTeam().getTrack().getId())
                .teamName(s.getTeam().getName())
                .repoUrl(s.getRepoUrl())
                .demoUrl(s.getDemoUrl())
                .slideUrl(s.getSlideUrl())
                .reportUrl(s.getReportUrl())
                .apiMetadata(s.getApiMetadata())
                .projectName(s.getProjectName())
                .version(s.getVersion())
                .reviewStatus(s.getReviewStatus())
                .status(s.getStatus())
                .submittedAt(s.getSubmittedAt())
                .updatedAt(s.getUpdatedAt())
                .recognitions(recognitions == null ? List.of() : recognitions)
                .build();
    }
}
