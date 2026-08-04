package vn.edu.fpt.seal.modules.resultversion.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.fpt.seal.modules.team.entity.Team;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "round_result_version_entries", uniqueConstraints = @UniqueConstraint(name = "uq_result_version_team", columnNames = {"result_version_id", "team_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoundResultVersionEntry {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "result_version_id")
    private RoundResultVersion resultVersion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id")
    private Team team;
    private Integer rank;
    @Column(name = "total_score")
    private BigDecimal totalScore;
    @Column(name = "promotion_status", nullable = false, length = 30)
    private String promotionStatus;
    @Column(name = "tie_breaker_score")
    private BigDecimal tieBreakerScore;
    @Column(name = "tie_breaker_reason", columnDefinition = "text")
    private String tieBreakerReason;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
