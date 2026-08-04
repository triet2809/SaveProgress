package vn.edu.fpt.seal.modules.resultversion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.resultversion.entity.RoundResultVersion;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoundResultVersionRepository extends JpaRepository<RoundResultVersion, UUID> {
    Optional<RoundResultVersion> findByRoundIdAndStatus(UUID roundId, String status);

    Optional<RoundResultVersion> findTopByRoundIdOrderByVersionNumberDesc(UUID roundId);

    boolean existsByRoundIdAndStatus(UUID roundId, String status);

    boolean existsByRoundId(UUID roundId);
}
