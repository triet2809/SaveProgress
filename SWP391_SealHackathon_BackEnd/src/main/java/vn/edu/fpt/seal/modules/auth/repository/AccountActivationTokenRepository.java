package vn.edu.fpt.seal.modules.auth.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.auth.entity.AccountActivationToken;
import java.util.*;

@Repository
public interface AccountActivationTokenRepository extends JpaRepository<AccountActivationToken, UUID> {
    Optional<AccountActivationToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from AccountActivationToken t where t.tokenHash = :tokenHash")
    Optional<AccountActivationToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("delete from AccountActivationToken t where t.user.id = :userId and t.usedAt is null")
    void deleteActiveByUserId(@Param("userId") UUID userId);
}
