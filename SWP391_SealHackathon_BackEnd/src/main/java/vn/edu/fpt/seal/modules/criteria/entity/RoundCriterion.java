package vn.edu.fpt.seal.modules.criteria.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import vn.edu.fpt.seal.modules.round.entity.Round;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "round_criteria", uniqueConstraints = @UniqueConstraint(name = "uq_round_criteria_round_name", columnNames = {"round_id", "name"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoundCriterion {
    @Id @GeneratedValue @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "round_id", nullable = false)
    private Round round;
    @Column(name = "template_id", columnDefinition = "uuid") private UUID templateId;
    @Column(name = "name", nullable = false, length = 255) private String name;
    @Column(name = "weight", nullable = false, precision = 10, scale = 2) private BigDecimal weight;
    @Column(name = "description", columnDefinition = "text") private String description;
    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "active";
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
}
