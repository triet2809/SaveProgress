package vn.edu.fpt.seal.modules.casework.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO trả về một "case" (vụ việc) cho client.
 * Case là lớp trừu tượng được ánh xạ từ IncidentReport (báo cáo sự cố),
 * với id dạng "INC-<uuid>" và trạng thái được chuẩn hóa sang thuật ngữ case.
 */
public record CaseResponse(String id, String source, String category, String subject, String description,
                           String status, UUID eventId, UUID roundId, UUID trackId, UUID teamId,
                           UUID submissionId, UUID reporterId, String reporterName,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
}
