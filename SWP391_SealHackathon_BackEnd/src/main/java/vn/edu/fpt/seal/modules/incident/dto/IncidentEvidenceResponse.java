package vn.edu.fpt.seal.modules.incident.dto;

import lombok.Builder; import java.time.LocalDateTime; import java.util.UUID;
@Builder public record IncidentEvidenceResponse(UUID id, UUID incidentId, String fileUrl, String externalUrl, String description, UUID uploadedBy, String uploadedByEmail, LocalDateTime createdAt) {}
