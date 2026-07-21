package vn.edu.fpt.seal.modules.criteria.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "criteria_templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CriteriaTemplate {
    @Id @GeneratedValue @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;
    @Column(name = "name", nullable = false, length = 255) private String name;
    @Column(name = "description", columnDefinition = "text") private String description;
    @Column(name = "default_weight", nullable = false, precision = 10, scale = 2) private BigDecimal defaultWeight;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
}
