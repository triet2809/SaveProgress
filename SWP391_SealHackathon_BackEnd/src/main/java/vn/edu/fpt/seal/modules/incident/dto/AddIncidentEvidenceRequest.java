package vn.edu.fpt.seal.modules.incident.dto;

import jakarta.validation.constraints.Size; import org.hibernate.validator.constraints.URL;
public record AddIncidentEvidenceRequest(@URL @Size(max=500) String fileUrl, @URL @Size(max=500) String externalUrl, @Size(max=10000) String description) {}
