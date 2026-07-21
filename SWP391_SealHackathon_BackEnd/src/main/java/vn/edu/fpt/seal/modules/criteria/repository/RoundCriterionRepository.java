package vn.edu.fpt.seal.modules.criteria.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.criteria.entity.RoundCriterion;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoundCriterionRepository extends JpaRepository<RoundCriterion, UUID> {
    Page<RoundCriterion> findByRoundId(UUID roundId, Pageable pageable);
    boolean existsByRoundIdAndNameIgnoreCase(UUID roundId, String name);
    boolean existsByRoundId(UUID roundId);
    @EntityGraph(attributePaths = {"round", "round.track", "round.track.event"})
    Optional<RoundCriterion> findWithRoundById(UUID id);
}
