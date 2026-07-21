package vn.edu.fpt.seal.modules.score.entity;

import jakarta.persistence.*; import lombok.*; import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.UpdateTimestamp;
import vn.edu.fpt.seal.modules.criteria.entity.RoundCriterion; import vn.edu.fpt.seal.modules.submission.entity.Submission; import vn.edu.fpt.seal.modules.user.entity.User;
import java.math.BigDecimal; import java.time.LocalDateTime; import java.util.UUID;

@Entity @Table(name="scores", uniqueConstraints=@UniqueConstraint(name="uq_scores_submission_judge_criterion", columnNames={"submission_id","judge_id","criterion_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Score {
    @Id @GeneratedValue @Column(name="id", updatable=false, nullable=false, columnDefinition="uuid") private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="submission_id", nullable=false) private Submission submission;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="judge_id", nullable=false) private User judge;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="criterion_id", nullable=false) private RoundCriterion criterion;
    @Column(name="score", nullable=false, precision=10, scale=2) private BigDecimal score;
    @Column(name="weighted_score", precision=10, scale=2) private BigDecimal weightedScore;
    @Column(name="criterion_average_score", precision=10, scale=2) private BigDecimal criterionAverageScore;
    @Column(name="criterion_variance", precision=10, scale=4) private BigDecimal criterionVariance;
    @Column(name="criterion_stddev", precision=10, scale=4) private BigDecimal criterionStddev;
    @Column(name="comment", columnDefinition="text") private String comment;
    @CreationTimestamp @Column(name="created_at", nullable=false, updatable=false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name="updated_at") private LocalDateTime updatedAt;
}
