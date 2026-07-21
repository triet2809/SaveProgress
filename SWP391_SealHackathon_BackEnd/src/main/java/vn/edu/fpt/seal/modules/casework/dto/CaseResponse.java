package vn.edu.fpt.seal.modules.casework.dto;
import java.time.LocalDateTime;
import java.util.UUID;
public record CaseResponse(String id, String source, String category, String subject, String description,
                           String status, UUID eventId, UUID roundId, UUID trackId, UUID teamId,
                           UUID submissionId, UUID reporterId, String reporterName,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {}
