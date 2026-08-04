package vn.edu.fpt.seal.modules.criteria.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import vn.edu.fpt.seal.modules.round.entity.Round;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Thực thể RoundCriterion — tiêu chí chấm điểm cụ thể của một vòng thi.
 * Có thể được tạo từ CriteriaTemplate (templateId) hoặc tạo riêng.
 * Ràng buộc duy nhất: (round_id, name) không trùng. Ánh xạ tới bảng "round_criteria".
 */
@Entity
@Table(name = "round_criteria", uniqueConstraints = @UniqueConstraint(name = "uq_round_criteria_round_name", columnNames = {"round_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoundCriterion {
    /**
     * Khóa chính UUID.
     */
    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;
    /**
     * Vòng thi chứa tiêu chí này.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;
    /**
     * Tham chiếu tới mẫu tiêu chí gốc (nếu được tạo từ template).
     */
    @Column(name = "template_id", columnDefinition = "uuid")
    private UUID templateId;
    /**
     * Tên tiêu chí.
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    /**
     * Trọng số tiêu chí trong tổng điểm.
     */
    @Column(name = "weight", nullable = false, precision = 10, scale = 2)
    private BigDecimal weight;
    /**
     * Mô tả tiêu chí.
     */
    @Column(name = "description", columnDefinition = "text")
    private String description;
    /**
     * Trạng thái tiêu chí; mặc định "active".
     */
    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "active";
    /**
     * Thời điểm tạo, tự sinh.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    /**
     * Thời điểm cập nhật gần nhất, tự sinh.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
