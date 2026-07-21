package vn.edu.fpt.seal.modules.round.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.fpt.seal.common.entity.BaseEntity;
import vn.edu.fpt.seal.modules.track.entity.Track;

import java.time.LocalDateTime;
import vn.edu.fpt.seal.common.enums.RoundLifecycleState;

@Entity
@Table(name = "rounds", uniqueConstraints = @UniqueConstraint(
        name = "uq_rounds_logical_track", columnNames = {"logical_round_id", "track_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Round extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "logical_round_id", nullable = false)
    private RoundDefinition logicalRound;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "submission_deadline", nullable = false)
    private LocalDateTime submissionDeadline;

    @Column(name = "top_n_to_promote", nullable = false)
    private Integer topNToPromote;

    @Column(name = "result_published_at")
    private LocalDateTime resultPublishedAt;

    @Column(name = "appeal_deadline")
    private LocalDateTime appealDeadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_state", nullable = false, length = 40)
    @Builder.Default
    private RoundLifecycleState lifecycleState = RoundLifecycleState.SCORING;

    @Version
    @Column(name = "lifecycle_version", nullable = false)
    private Long lifecycleVersion;
}
