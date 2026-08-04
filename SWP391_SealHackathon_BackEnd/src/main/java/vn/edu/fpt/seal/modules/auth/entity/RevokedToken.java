package vn.edu.fpt.seal.modules.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;
import vn.edu.fpt.seal.common.entity.BaseEntity;

import java.time.Instant;

/**
 * Entity lưu trữ các token đã bị thu hồi (blacklist).
 * Dùng để vô hiệu hóa JWT trước khi hết hạn (ví dụ khi đăng xuất).
 * Bản ghi hết hạn được dọn định kỳ dựa trên {@code expiresAt}.
 */
@Entity
@Table(name = "revoked_tokens", indexes = {
        @Index(name = "idx_revoked_tokens_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_revoked_tokens_expires_at", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevokedToken extends BaseEntity {
    /**
     * Giá trị băm (hash) của token bị thu hồi — không lưu token gốc vì lý do bảo mật.
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /**
     * Thời điểm token hết hạn — sau mốc này bản ghi có thể xóa khỏi blacklist.
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
