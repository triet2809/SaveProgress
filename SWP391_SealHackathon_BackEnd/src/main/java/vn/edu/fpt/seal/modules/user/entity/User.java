package vn.edu.fpt.seal.modules.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.edu.fpt.seal.common.entity.BaseEntity;
import vn.edu.fpt.seal.common.enums.AccountStatus;
import vn.edu.fpt.seal.common.enums.StudentType;
import vn.edu.fpt.seal.modules.university.entity.Campus;
import vn.edu.fpt.seal.modules.university.entity.University;

import java.util.HashSet;
import java.util.Set;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "google_sub", unique = true, length = 255)
    private String googleSub;

    @Column(name = "auth_provider", nullable = false, length = 20)
    @Builder.Default
    private String authProvider = "local";

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "student_type", nullable = false, columnDefinition = "student_type")
    @Builder.Default
    private StudentType studentType = StudentType.none;

    @Column(name = "student_id", length = 100)
    private String studentId;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "department", length = 255)
    private String department;

    @Column(name = "position", length = 255)
    private String position;

    @Column(name = "company", length = 255)
    private String company;

    @Column(name = "expertise", length = 255)
    private String expertise;

    @Column(name = "bio", columnDefinition = "text")
    private String bio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id")
    private University university;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id")
    private Campus campus;

    @Column(name = "is_guest", nullable = false)
    @Builder.Default
    private boolean isGuest = false;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "account_status")
    @Builder.Default
    private AccountStatus status = AccountStatus.pending;

    @Column(name = "security_version", nullable = false)
    @Builder.Default
    private long securityVersion = 1L;

    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private boolean mustChangePassword = false;

    @Column(name = "terms_accepted_at")
    private LocalDateTime termsAcceptedAt;

    @Column(name = "terms_version", length = 100)
    private String termsVersion;

    @Column(name = "privacy_version", length = 100)
    private String privacyVersion;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    public void incrementSecurityVersion() {
        securityVersion++;
    }

    public void setStatus(AccountStatus status) {
        if (this.status != status && this.securityVersion > 0) {
            this.securityVersion++;
        }
        this.status = status;
    }
}
