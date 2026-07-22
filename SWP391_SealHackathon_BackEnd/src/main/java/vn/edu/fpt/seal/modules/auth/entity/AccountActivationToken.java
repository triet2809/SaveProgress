package vn.edu.fpt.seal.modules.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.fpt.seal.modules.user.entity.User;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity lưu token kích hoạt tài khoản gửi qua email cho người dùng mới.
 * Mỗi token gắn với một {@link User}, có thời hạn và chỉ dùng được một lần.
 */
@Entity
@Table(name = "account_activation_tokens", indexes = {
        @Index(name = "idx_activation_user", columnList = "user_id"),
        @Index(name = "idx_activation_expires", columnList = "expires_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountActivationToken {
    /** Khóa chính, sinh tự động dạng UUID. */
    @Id @GeneratedValue @Column(columnDefinition = "uuid") private UUID id;
    /** Người dùng sở hữu token; tải lazy để tránh lấy dư dữ liệu. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    /** Hash của token — không lưu token gốc vì lý do bảo mật. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    /** Thời điểm token hết hạn. */
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
    /** Thời điểm token được sử dụng; null nếu chưa dùng. */
    @Column(name = "used_at") private LocalDateTime usedAt;
    /** Thời điểm tạo token. */
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    /** ID người tạo token (nếu do admin cấp). */
    @Column(name = "created_by") private UUID createdBy;
}
