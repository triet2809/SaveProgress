package vn.edu.fpt.seal.modules.submission.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.team.entity.Team;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "submissions", uniqueConstraints = @UniqueConstraint(name = "uq_submissions_round_team", columnNames = {"round_id", "team_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submission {
    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "repo_url", length = 500)
    private String repoUrl;
    @Column(name = "demo_url", length = 500)
    private String demoUrl;
    @Column(name = "slide_url", length = 500)
    private String slideUrl;
    @Column(name = "report_url", length = 500)
    private String reportUrl;
    @Column(name = "api_metadata", columnDefinition = "text")
    private String apiMetadata;
    @Column(name = "project_name", length = 255)
    private String projectName;
    @Column(name = "version", length = 50)
    private String version;
    @Column(name = "review_status", length = 50)
    @Builder.Default
    private String reviewStatus = "pending";

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "draft";

    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
