package vn.edu.fpt.seal.modules.seeding.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.teamprofile.entity.TeamProfile;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.user.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "event_seed_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSeedAssignment {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private Event event;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "track_id")
    private Track track;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id")
    private Team team;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_profile_id")
    private TeamProfile teamProfile;
    @Column(name = "competition_stage", nullable = false, length = 40)
    @Builder.Default
    private String competitionStage = "event_setup";
    @Column(name = "seed_number")
    private Integer seedNumber;
    @Column(name = "seed_tier", length = 20)
    private String seedTier;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_source_finish_id")
    private EventTeamFinish candidateSourceFinish;
    @Column(name = "continuity_count", nullable = false)
    private Integer continuityCount;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(columnDefinition = "text")
    private String rationale;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;
    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;
}
