package vn.edu.fpt.seal.modules.participant.dto;

import lombok.Builder;
import vn.edu.fpt.seal.common.enums.RoundParticipantStatus;
import vn.edu.fpt.seal.modules.recognition.dto.RecognitionDtos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record RoundParticipantResponse(UUID id, UUID roundId, String roundName, UUID teamId, String teamName,
                                       RoundParticipantStatus status, String note, LocalDateTime createdAt,
                                       LocalDateTime updatedAt, List<RecognitionDtos.Summary> recognitions) {
}
