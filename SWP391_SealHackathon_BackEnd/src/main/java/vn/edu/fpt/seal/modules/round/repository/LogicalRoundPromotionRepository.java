package vn.edu.fpt.seal.modules.round.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.seal.modules.round.entity.LogicalRoundPromotion;

import java.util.List;
import java.util.UUID;

public interface LogicalRoundPromotionRepository extends JpaRepository<LogicalRoundPromotion, UUID> {
    boolean existsByTargetLogicalRoundIdAndTeamId(UUID targetLogicalRoundId, UUID teamId);
    boolean existsBySourceLogicalRoundId(UUID sourceLogicalRoundId);
    boolean existsByTargetLogicalRoundId(UUID targetLogicalRoundId);

    @EntityGraph(attributePaths = {"sourceLogicalRound", "targetLogicalRound", "team"})
    List<LogicalRoundPromotion> findByTargetLogicalRoundIdOrderByTeamNameAsc(UUID targetLogicalRoundId);
}
