package vn.edu.fpt.seal.modules.judge.dto;

import lombok.Builder;
import vn.edu.fpt.seal.modules.recognition.dto.RecognitionDtos;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record JudgeSubmissionResponse(UUID roundJudgeId, UUID judgeId, UUID eventId, String eventName,
                                      UUID trackId, String trackName, UUID roundId, String roundName,
                                      UUID teamId, String teamName, UUID submissionId, String repoUrl,
                                      String presentationUrl, String demoUrl, String status, String reviewStatus,
                                      LocalDateTime submittedAt, List<RecognitionDtos.Summary> recognitions) {}
