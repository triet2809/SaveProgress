package vn.edu.fpt.seal.modules.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateSupportTicketStatusRequest(
        @NotBlank
        @Pattern(regexp = "open|in_progress|resolved",
                message = "status must be open, in_progress, or resolved")
        String status
) {
}
