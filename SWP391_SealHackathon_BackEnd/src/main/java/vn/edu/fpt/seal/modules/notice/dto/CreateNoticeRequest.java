package vn.edu.fpt.seal.modules.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateNoticeRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank String content,
        @Size(max = 20) String priority,
        @Size(max = 100) String targetRole,
        UUID targetEventId,
        UUID targetTrackId,
        UUID targetTeamId
) {}
