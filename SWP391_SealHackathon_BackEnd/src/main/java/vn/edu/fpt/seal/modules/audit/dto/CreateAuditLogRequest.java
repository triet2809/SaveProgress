package vn.edu.fpt.seal.modules.audit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.edu.fpt.seal.common.enums.AuditAction;

import java.util.UUID;

/**
 * DTO đầu vào tạo bản ghi nhật ký kiểm toán.
 * userId/teamId/incidentId là tuỳ chọn; action, targetType, targetId bắt buộc.
 * oldValue/newValue/details giới hạn 10000 ký tự.
 */
public record CreateAuditLogRequest(UUID userId, UUID teamId, UUID incidentId, @NotNull AuditAction action,
                                    @NotBlank @Size(max = 100) String targetType, @NotNull UUID targetId,
                                    @Size(max = 10000) String oldValue, @Size(max = 10000) String newValue,
                                    @Size(max = 10000) String details) {
}
