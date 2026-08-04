package vn.edu.fpt.seal.modules.criteria.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.criteria.entity.RoundCriterion;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository truy xuất tiêu chí chấm điểm theo vòng thi (RoundCriterion).
 */
@Repository
public interface RoundCriterionRepository extends JpaRepository<RoundCriterion, UUID> {
    /** Lấy các tiêu chí của một vòng thi, có phân trang. */
    Page<RoundCriterion> findByRoundId(UUID roundId, Pageable pageable);
    /** Kiểm tra trùng tên tiêu chí trong cùng vòng thi (không phân biệt hoa thường). */
    boolean existsByRoundIdAndNameIgnoreCase(UUID roundId, String name);
    /** Kiểm tra vòng thi đã có tiêu chí nào chưa. */
    boolean existsByRoundId(UUID roundId);
    /** Lấy tiêu chí kèm nạp sẵn round/track/event để phục vụ kiểm tra quyền theo phạm vi. */
    @EntityGraph(attributePaths = {"round", "round.track", "round.track.event"})
    Optional<RoundCriterion> findWithRoundById(UUID id);
    /** Tính tổng trọng số của các tiêu chí trong một vòng thi; trả về 0 nếu chưa có tiêu chí nào. */
    @Query("SELECT COALESCE(SUM(c.weight), 0) FROM RoundCriterion c WHERE c.round.id = :roundId")
    BigDecimal sumWeightByRoundId(@Param("roundId") UUID roundId);
}
