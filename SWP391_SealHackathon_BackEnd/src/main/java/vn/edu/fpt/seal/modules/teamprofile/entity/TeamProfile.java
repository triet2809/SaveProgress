package vn.edu.fpt.seal.modules.teamprofile.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.fpt.seal.common.entity.BaseEntity;
import vn.edu.fpt.seal.common.enums.TeamProfileStatus;
import vn.edu.fpt.seal.modules.user.entity.User;

@Entity
@Table(name = "team_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamProfile extends BaseEntity {
    @Column(name = "canonical_name", nullable = false, length = 255)
    private String canonicalName;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TeamProfileStatus status = TeamProfileStatus.active;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;
}
