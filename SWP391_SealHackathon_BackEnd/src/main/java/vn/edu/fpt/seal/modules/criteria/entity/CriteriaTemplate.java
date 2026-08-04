package vn.edu.fpt.seal.modules.criteria.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Thực thể CriteriaTemplate — mẫu tiêu chí chấm điểm dùng chung.
 * Lưu trữ tiêu chí mẫu (tên, mô tả, trọng số mặc định) để tái sử dụng khi tạo tiêu chí cho từng vòng thi.
 * Ánh xạ tới bảng "criteria_templates".
 */
@Entity
@Table(name = "criteria_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CriteriaTemplate {
    /**
     * Khóa chính UUID.
     */
    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;
    /**
     * Tên tiêu chí mẫu.
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    /**
     * Mô tả tiêu chí.
     */
    @Column(name = "description", columnDefinition = "text")
    private String description;
    /**
     * Trọng số mặc định dùng khi áp mẫu vào vòng thi.
     */
    @Column(name = "default_weight", nullable = false, precision = 10, scale = 2)
    private BigDecimal defaultWeight;
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
