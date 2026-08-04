package vn.edu.fpt.seal.modules.round.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import vn.edu.fpt.seal.modules.team.entity.Team;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "logical_round_promotions", uniqueConstraints = @UniqueConstraint(
        name = "uq_logical_round_promotions_target_team",
        columnNames = {"target_logical_round_id", "team_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogicalRoundPromotion {
    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_logical_round_id", nullable = false)
    private RoundDefinition sourceLogicalRound;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_logical_round_id", nullable = false)
    private RoundDefinition targetLogicalRound;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    /**
     * Immutable published-result provenance used to justify this promotion.
     */
    @Column(name = "source_result_version_id", nullable = false, updatable = false)
    private UUID sourceResultVersionId;

    @Column(name = "source_result_entry_id", nullable = false, updatable = false)
    private UUID sourceResultEntryId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
