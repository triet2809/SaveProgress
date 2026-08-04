package vn.edu.fpt.seal.modules.auth.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.auth.entity.AccountActivationToken;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository truy xuất token kích hoạt tài khoản.
 */
@Repository
public interface AccountActivationTokenRepository extends JpaRepository<AccountActivationToken, UUID> {
    /**
     * Tìm token theo hash.
     *
     * @param tokenHash hash của token
     * @return token nếu tồn tại
     */
    Optional<AccountActivationToken> findByTokenHash(String tokenHash);

    /**
     * Tìm token theo hash và khóa bản ghi (pessimistic write lock)
     * để tránh race condition khi hai request cùng kích hoạt một token.
     *
     * @param tokenHash hash của token
     * @return token đã khóa nếu tồn tại
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from AccountActivationToken t where t.tokenHash = :tokenHash")
    Optional<AccountActivationToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    /**
     * Xóa các token chưa dùng của một người dùng (ví dụ khi cấp lại token mới).
     *
     * @param userId ID người dùng
     */
    @Modifying
    @Query("delete from AccountActivationToken t where t.user.id = :userId and t.usedAt is null")
    void deleteActiveByUserId(@Param("userId") UUID userId);
}
