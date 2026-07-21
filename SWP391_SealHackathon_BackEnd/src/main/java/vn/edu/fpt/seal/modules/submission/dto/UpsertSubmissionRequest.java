package vn.edu.fpt.seal.modules.submission.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.util.UUID;

public record UpsertSubmissionRequest(
        @NotNull UUID roundId,
        @NotNull UUID teamId,
        @URL @Size(max = 500) String repoUrl,
        @URL @Size(max = 500) String demoUrl,
        @URL @Size(max = 500) String slideUrl,
        @URL @Size(max = 500) String reportUrl,
        @Size(max = 10000) String apiMetadata,
        @Size(max = 255) String projectName,
        @Size(max = 50) String version,
        @Size(max = 50) String reviewStatus
) {}
