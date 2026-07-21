package vn.edu.fpt.seal.modules.incident.dto;

import lombok.Builder; import vn.edu.fpt.seal.common.enums.IncidentActionType; import java.time.LocalDateTime; import java.util.UUID;
@Builder public record IncidentActionResponse(UUID id, UUID incidentId, UUID actionBy, String actionByEmail, IncidentActionType actionType, String targetType, UUID targetId, String oldValue, String newValue, String note, LocalDateTime createdAt) {}
