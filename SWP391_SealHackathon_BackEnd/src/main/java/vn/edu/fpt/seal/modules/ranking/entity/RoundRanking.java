package vn.edu.fpt.seal.modules.ranking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import vn.edu.fpt.seal.common.enums.PromotionStatus;
import vn.edu.fpt.seal.modules.criteria.entity.RoundCriterion;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.team.entity.Team;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "round_rankings", uniqueConstraints = {
        @UniqueConstraint(name = "uq_round_rankings_round_team", columnNames = {"round_id", "team_id"}),
        @UniqueConstraint(name = "uq_round_rankings_round_rank", columnNames = {"round_id", "rank"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoundRanking {
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
    @Column(name = "total_score", precision = 10, scale = 2)
    private BigDecimal totalScore;
    @Column(name = "rank")
    private Integer rank;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "promotion_status")
    @Builder.Default
    private PromotionStatus status = PromotionStatus.pending;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tie_breaker_criterion_id")
    private RoundCriterion tieBreakerCriterion;
    @Column(name = "tie_breaker_score", precision = 10, scale = 2)
    private BigDecimal tieBreakerScore;
    @Column(name = "tie_breaker_reason", columnDefinition = "text")
    private String tieBreakerReason;
    @CreationTimestamp
    @Column(name = "calculated_at", nullable = false, updatable = false)
    private LocalDateTime calculatedAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
