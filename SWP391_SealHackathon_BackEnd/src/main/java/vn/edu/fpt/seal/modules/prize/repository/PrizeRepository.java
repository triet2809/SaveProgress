package vn.edu.fpt.seal.modules.prize.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.prize.entity.Prize;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrizeRepository extends JpaRepository<Prize, UUID> {
    @EntityGraph(attributePaths = {"event", "track", "team"})
    Page<Prize> findByEventId(UUID eventId, Pageable p);

    @EntityGraph(attributePaths = {"event", "track", "team"})
    Page<Prize> findByTrackId(UUID trackId, Pageable p);

    @EntityGraph(attributePaths = {"event", "track", "team"})
    Page<Prize> findByTeamId(UUID teamId, Pageable p);

    @EntityGraph(attributePaths = {"event", "track", "team"})
    Optional<Prize> findWithRelationsById(UUID id);

    boolean existsByEventIdAndTeamIdAndNameIgnoreCase(UUID eventId, UUID teamId, String name);

    boolean existsByEventIdAndTeamIdAndNameIgnoreCaseAndIdNot(UUID eventId, UUID teamId, String name, UUID id);
}
