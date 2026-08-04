package vn.edu.fpt.seal.modules.audit.mapper;

import vn.edu.fpt.seal.modules.audit.dto.AuditLogResponse;
import vn.edu.fpt.seal.modules.audit.entity.AuditLog;

/**
 * Mapper chuyển thực thể AuditLog sang DTO AuditLogResponse.
 * Lớp tiện ích tĩnh, không cho khởi tạo.
 */
public final class AuditLogMapper {
    private AuditLogMapper() {
    }

    /**
     * Ánh xạ AuditLog sang AuditLogResponse.
     * Xử lý null-safe cho các quan hệ user/team/incident.
     *
     * @param a thực thể nguồn
     * @return DTO tương ứng
     */
    public static AuditLogResponse toResponse(AuditLog a) {
        return AuditLogResponse.builder().id(a.getId()).userId(a.getUser() == null ? null : a.getUser().getId()).userEmail(a.getUser() == null ? null : a.getUser().getEmail()).teamId(a.getTeam() == null ? null : a.getTeam().getId()).teamName(a.getTeam() == null ? null : a.getTeam().getName()).incidentId(a.getIncident() == null ? null : a.getIncident().getId()).action(a.getAction()).targetType(a.getTargetType()).targetId(a.getTargetId()).oldValue(a.getOldValue()).newValue(a.getNewValue()).details(a.getDetails()).occurredAt(a.getOccurredAt()).build();
    }
}
