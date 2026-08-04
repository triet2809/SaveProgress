package vn.edu.fpt.seal.modules.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSupportTicketRequest(@NotBlank @Size(max = 50) String category,
                                         @NotBlank @Size(max = 20) String priority,
                                         @NotBlank @Size(max = 255) String subject,
                                         @NotBlank @Size(max = 10000) String description) {
}
