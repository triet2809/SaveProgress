package vn.edu.fpt.seal.modules.incident.dto;

import jakarta.validation.constraints.*; import vn.edu.fpt.seal.common.enums.IncidentType; import java.util.UUID;
public record CreateIncidentRequest(@NotNull UUID eventId, UUID trackId, UUID roundId, UUID teamId, UUID submissionId, @NotNull IncidentType type, @Size(max=50) String severity, @Size(max=100) String category, @NotBlank @Size(max=255) String title, @NotBlank @Size(max=10000) String description) {}
