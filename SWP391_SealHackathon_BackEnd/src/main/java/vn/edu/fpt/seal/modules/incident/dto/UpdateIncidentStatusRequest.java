package vn.edu.fpt.seal.modules.incident.dto;

import jakarta.validation.constraints.*; import vn.edu.fpt.seal.common.enums.IncidentStatus; import java.util.UUID;
public record UpdateIncidentStatusRequest(@NotNull IncidentStatus status, UUID assignedCoordinatorId, @Size(max=10000) String note) {}
