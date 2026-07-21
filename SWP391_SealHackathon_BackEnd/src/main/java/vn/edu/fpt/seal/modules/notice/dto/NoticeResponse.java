package vn.edu.fpt.seal.modules.notice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record NoticeResponse(UUID id, String title, String content, String priority, String targetRole, UUID targetEventId, UUID targetTrackId, UUID targetTeamId, UUID authorId, String authorEmail, String authorName, LocalDateTime createdAt, LocalDateTime updatedAt) {}
