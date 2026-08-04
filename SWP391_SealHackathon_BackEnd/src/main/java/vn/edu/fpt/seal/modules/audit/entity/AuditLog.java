package vn.edu.fpt.seal.modules.audit.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.edu.fpt.seal.common.enums.AuditAction;
import vn.edu.fpt.seal.modules.incident.entity.IncidentReport;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.user.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Thực thể AuditLog — bản ghi nhật ký kiểm toán (audit trail).
 * Ghi lại các thao tác quan trọng: ai (user), ở đâu (team/incident), hành động gì (action),
 * trên đối tượng nào (targetType/targetId), giá trị cũ/mới để truy vết thay đổi.
 * Ánh xạ tới bảng "audit_logs".
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    /**
     * Khóa chính UUID, không cho cập nhật.
     */
    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;
    /**
     * Người thực hiện thao tác (có thể null nếu do hệ thống).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    /**
     * Đội liên quan (nếu có).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;
    /**
     * Sự cố liên quan (nếu có).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private IncidentReport incident;
    /**
     * Loại hành động (enum audit_action trong DB).
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "action", nullable = false, columnDefinition = "audit_action")
    private AuditAction action;
    /**
     * Loại đối tượng bị tác động (ví dụ "Team", "Score").
     */
    @Column(name = "target_type", nullable = false, length = 100)
    private String targetType;
    /**
     * ID đối tượng bị tác động.
     */
    @Column(name = "target_id", nullable = false, columnDefinition = "uuid")
    private UUID targetId;
    /**
     * Giá trị trước khi thay đổi (JSON/text).
     */
    @Column(name = "old_value", columnDefinition = "text")
    private String oldValue;
    /**
     * Giá trị sau khi thay đổi (JSON/text).
     */
    @Column(name = "new_value", columnDefinition = "text")
    private String newValue;
    /**
     * Mô tả chi tiết bổ sung.
     */
    @Column(name = "details", columnDefinition = "text")
    private String details;
    /**
     * Thời điểm xảy ra, tự sinh khi tạo, không cập nhật.
     */
    @CreationTimestamp
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;
}
