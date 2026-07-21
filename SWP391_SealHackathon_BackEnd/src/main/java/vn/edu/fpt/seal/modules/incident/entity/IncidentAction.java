package vn.edu.fpt.seal.modules.incident.entity;

import jakarta.persistence.*; import lombok.*; import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.UpdateTimestamp; import org.hibernate.annotations.JdbcTypeCode; import org.hibernate.type.SqlTypes; import vn.edu.fpt.seal.common.enums.IncidentActionType; import vn.edu.fpt.seal.modules.user.entity.User;
import java.time.LocalDateTime; import java.util.UUID;
@Entity @Table(name="incident_actions") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IncidentAction {
 @Id @GeneratedValue @Column(name="id",updatable=false,nullable=false,columnDefinition="uuid") private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="incident_id",nullable=false) private IncidentReport incident;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="action_by",nullable=false) private User actionBy;
 @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM) @Column(name="action_type",nullable=false,columnDefinition="incident_action_type") private IncidentActionType actionType;
 @Column(name="target_type",length=100) private String targetType; @Column(name="target_id",columnDefinition="uuid") private UUID targetId; @Column(name="old_value",columnDefinition="text") private String oldValue; @Column(name="new_value",columnDefinition="text") private String newValue; @Column(name="note",columnDefinition="text") private String note;
 @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
}
