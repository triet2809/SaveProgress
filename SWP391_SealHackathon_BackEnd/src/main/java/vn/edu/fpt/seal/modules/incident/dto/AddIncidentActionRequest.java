package vn.edu.fpt.seal.modules.incident.dto;

import jakarta.validation.constraints.*; import vn.edu.fpt.seal.common.enums.IncidentActionType; import java.util.UUID;
public record AddIncidentActionRequest(@NotNull IncidentActionType actionType, @Size(max=100) String targetType, UUID targetId, @Size(max=10000) String oldValue, @Size(max=10000) String newValue, @Size(max=10000) String note) {}
