package vn.edu.fpt.seal.modules.incident.entity;

import jakarta.persistence.*; import lombok.*; import org.hibernate.annotations.CreationTimestamp; import vn.edu.fpt.seal.modules.user.entity.User;
import java.time.LocalDateTime; import java.util.UUID;
@Entity @Table(name="incident_evidences") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IncidentEvidence {
 @Id @GeneratedValue @Column(name="id",updatable=false,nullable=false,columnDefinition="uuid") private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="incident_id",nullable=false) private IncidentReport incident;
 @Column(name="file_url",length=500) private String fileUrl; @Column(name="external_url",length=500) private String externalUrl; @Column(name="description",columnDefinition="text") private String description;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="uploaded_by",nullable=false) private User uploadedBy;
 @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
}
