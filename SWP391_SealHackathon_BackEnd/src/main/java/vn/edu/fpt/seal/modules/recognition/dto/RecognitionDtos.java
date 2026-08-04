package vn.edu.fpt.seal.modules.recognition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class RecognitionDtos {
    private RecognitionDtos() {
    }

    public record Summary(
            String code,
            String label,
            String displayText,
            int qualificationCount,
            LocalDateTime earnedAt,
            boolean active
    ) {
    }

    public record QualifyingSeason(
            UUID finishId,
            UUID eventId,
            String eventName,
            UUID trackId,
            String trackName,
            int finalPlacement,
            UUID resultVersionId,
            LocalDateTime completedAt
    ) {
    }

    public record EvidenceResponse(
            UUID teamProfileId,
            UUID recognitionId,
            Summary recognition,
            int distinctQualifyingSeasons,
            List<QualifyingSeason> qualifyingSeasons
    ) {
    }

    public record RevokeRequest(
            @NotBlank @Size(max = 4000) String reason
    ) {
    }
}
