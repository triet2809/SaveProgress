package vn.edu.fpt.seal.modules.appeal.repository;
import org.springframework.data.jpa.repository.*; import org.springframework.stereotype.Repository; import vn.edu.fpt.seal.modules.appeal.entity.Appeal; import java.util.*;
/**
 * Repository truy xuất dữ liệu cho thực thể Appeal (khiếu nại kết quả).
 * Cung cấp các truy vấn kiểm tra trùng lặp và lấy danh sách khiếu nại theo sự kiện/đội.
 */
@Repository public interface AppealRepository extends JpaRepository<Appeal,UUID>{
 /** Kiểm tra đội đã khiếu nại cho phiên bản kết quả này chưa (chống khiếu nại trùng). */
 boolean existsByTeamIdAndResultVersionId(UUID teamId,UUID versionId);
 /** Kiểm tra vòng thi có khiếu nại nào ở trạng thái cụ thể không. */
 boolean existsByRoundIdAndStatus(UUID roundId,String status);
 /** Kiểm tra vòng thi có tồn tại khiếu nại nào không. */
 boolean existsByRoundId(UUID roundId);
 /** Lấy tất cả khiếu nại của một sự kiện, sắp xếp mới nhất trước. */
 List<Appeal> findByEventIdOrderByCreatedAtDesc(UUID eventId);
 /** Lấy tất cả khiếu nại của một đội, sắp xếp mới nhất trước. */
 List<Appeal> findByTeamIdOrderByCreatedAtDesc(UUID teamId);
 /** Lấy khiếu nại kèm nạp sẵn (eager) các quan hệ liên quan để tránh lỗi lazy-loading. */
 @EntityGraph(attributePaths={"event","round","team","submittedBy","resolvedBy","resultVersion"}) Optional<Appeal> findWithRelationsById(UUID id);
}
