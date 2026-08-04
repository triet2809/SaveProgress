package vn.edu.fpt.seal.modules.audit.dto;

import lombok.Builder;
import vn.edu.fpt.seal.common.enums.AuditAction;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO trả về một bản ghi nhật ký kiểm toán cho client.
 * Kèm thông tin rút gọn của user (email) và team (tên) để hiển thị.
 */
@Builder
public record AuditLogResponse(UUID id, UUID userId, String userEmail, UUID teamId, String teamName, UUID incidentId,
                               AuditAction action, String targetType, UUID targetId, String oldValue, String newValue,
                               String details, LocalDateTime occurredAt) {
}
