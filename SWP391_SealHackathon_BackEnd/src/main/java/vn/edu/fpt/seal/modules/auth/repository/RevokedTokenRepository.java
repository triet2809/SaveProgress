package vn.edu.fpt.seal.modules.auth.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.auth.entity.RevokedToken;
import java.time.Instant;
import java.util.UUID;

@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, UUID> {
    boolean existsByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);
    void deleteByExpiresAtBefore(Instant now);
}
