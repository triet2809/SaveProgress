package vn.edu.fpt.seal.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lớp entity cơ sở dùng chung cho mọi entity JPA trong hệ thống.
 * Cung cấp khóa chính dạng UUID và các trường audit thời gian tạo/cập nhật.
 *
 * <p>Được đánh dấu {@link MappedSuperclass} nên các trường ở đây được ánh xạ trực tiếp
 * vào bảng của entity con mà không tạo bảng riêng. {@link AuditingEntityListener}
 * tự động gán giá trị cho createdAt/updatedAt (cần bật {@code @EnableJpaAuditing}).</p>
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /** Khóa chính dạng UUID, sinh tự động, không cho phép cập nhật sau khi tạo. */
    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    /** Thời điểm tạo bản ghi, gán tự động một lần khi tạo, không thay đổi sau đó. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Thời điểm cập nhật gần nhất, gán tự động mỗi lần lưu thay đổi. */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
