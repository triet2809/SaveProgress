package vn.edu.fpt.seal.modules.recognition.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import vn.edu.fpt.seal.modules.teamprofile.entity.TeamProfile;
import vn.edu.fpt.seal.modules.user.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "team_recognitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamRecognition {
    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_profile_id", nullable = false)
    private TeamProfile teamProfile;

    @Column(name = "recognition_code", nullable = false, length = 80)
    private String recognitionCode;

    @Column(name = "label", nullable = false, length = 160)
    private String label;

    @Column(name = "qualification_count", nullable = false)
    private Integer qualificationCount;

    @Column(name = "earned_at", nullable = false)
    private LocalDateTime earnedAt;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by")
    private User revokedBy;

    @Column(name = "revoke_reason", columnDefinition = "text")
    private String revokeReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;
}
