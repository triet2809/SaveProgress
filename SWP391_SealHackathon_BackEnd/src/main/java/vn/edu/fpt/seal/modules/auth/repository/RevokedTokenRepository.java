package vn.edu.fpt.seal.modules.auth.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.auth.entity.RevokedToken;
import java.time.Instant;
import java.util.UUID;

/**
 * Repository truy xuất bảng token đã thu hồi (blacklist JWT).
 */
@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, UUID> {
    /**
     * Kiểm tra token có nằm trong blacklist và vẫn còn hiệu lực (chưa hết hạn) hay không.
     *
     * @param tokenHash hash của token cần kiểm tra
     * @param now       mốc thời gian hiện tại để so sánh hạn
     * @return true nếu token đã bị thu hồi và chưa hết hạn
     */
    boolean existsByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);

    /**
     * Xóa các bản ghi token đã hết hạn để dọn dẹp blacklist.
     *
     * @param now mốc thời gian; mọi bản ghi hết hạn trước mốc này sẽ bị xóa
     */
    void deleteByExpiresAtBefore(Instant now);
}
