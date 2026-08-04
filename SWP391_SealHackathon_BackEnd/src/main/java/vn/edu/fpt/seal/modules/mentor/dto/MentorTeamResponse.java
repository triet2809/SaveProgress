package vn.edu.fpt.seal.modules.mentor.dto;

import lombok.Builder;
import vn.edu.fpt.seal.common.enums.TeamStatus;
import vn.edu.fpt.seal.modules.recognition.dto.RecognitionDtos;

import java.util.List;
import java.util.UUID;

@Builder
public record MentorTeamResponse(UUID trackMentorId, UUID mentorId, UUID eventId, String eventName,
                                 UUID trackId, String trackName, UUID roundId, String roundName,
                                 UUID teamId, String teamName, TeamStatus teamStatus,
                                 List<RecognitionDtos.Summary> recognitions) {
}
