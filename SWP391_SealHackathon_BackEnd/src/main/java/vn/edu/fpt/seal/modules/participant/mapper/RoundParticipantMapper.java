package vn.edu.fpt.seal.modules.participant.mapper;

import vn.edu.fpt.seal.modules.participant.dto.RoundParticipantResponse;
import vn.edu.fpt.seal.modules.participant.entity.RoundParticipant;
import vn.edu.fpt.seal.modules.recognition.dto.RecognitionDtos;

import java.util.List;

public final class RoundParticipantMapper {
    private RoundParticipantMapper() {
    }

    public static RoundParticipantResponse toResponse(RoundParticipant p) {
        return toResponse(p, List.of());
    }

    public static RoundParticipantResponse toResponse(RoundParticipant p, List<RecognitionDtos.Summary> recognitions) {
        return RoundParticipantResponse.builder().id(p.getId()).roundId(p.getRound().getId()).roundName(p.getRound().getName()).teamId(p.getTeam().getId()).teamName(p.getTeam().getName()).status(p.getStatus()).note(p.getNote()).createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt()).recognitions(recognitions == null ? List.of() : recognitions).build();
    }
}
