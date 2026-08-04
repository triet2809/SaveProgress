package vn.edu.fpt.seal.modules.round.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.fpt.seal.common.entity.BaseEntity;
import vn.edu.fpt.seal.common.enums.RoundLifecycleState;
import vn.edu.fpt.seal.modules.event.entity.Event;

@Entity
@Table(name = "round_definitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoundDefinition extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "is_final", nullable = false)
    @Builder.Default
    private boolean finalRound = false;

    @Column(name = "default_top_n_to_promote")
    private Integer defaultTopNToPromote;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_state", nullable = false, length = 40)
    @Builder.Default
    private RoundLifecycleState lifecycleState = RoundLifecycleState.SCORING;
}
