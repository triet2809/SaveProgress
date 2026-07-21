package vn.edu.fpt.seal.modules.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.fpt.seal.modules.user.entity.User;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "account_activation_tokens", indexes = {
        @Index(name = "idx_activation_user", columnList = "user_id"),
        @Index(name = "idx_activation_expires", columnList = "expires_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountActivationToken {
    @Id @GeneratedValue @Column(columnDefinition = "uuid") private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
    @Column(name = "used_at") private LocalDateTime usedAt;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "created_by") private UUID createdBy;
}
