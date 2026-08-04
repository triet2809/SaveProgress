package vn.edu.fpt.seal.modules.incident.dto;

import lombok.Builder;
import vn.edu.fpt.seal.common.enums.IncidentStatus;
import vn.edu.fpt.seal.common.enums.IncidentType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record IncidentResponse(UUID id, UUID eventId, UUID trackId, UUID roundId, UUID teamId, UUID submissionId,
                               UUID reporterId, String reporterEmail, UUID assignedCoordinatorId,
                               String assignedCoordinatorEmail, IncidentType type, IncidentStatus status,
                               String severity, String category, String title, String description,
                               LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime resolvedAt,
                               List<IncidentEvidenceResponse> evidences, List<IncidentActionResponse> actions) {
}
