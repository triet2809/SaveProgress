package vn.edu.fpt.seal.modules.participant.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.common.enums.RoundParticipantStatus;
import vn.edu.fpt.seal.modules.participant.entity.RoundParticipant;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoundParticipantRepository extends JpaRepository<RoundParticipant, UUID> {
    @EntityGraph(attributePaths = {"round", "team"})
    Page<RoundParticipant> findByRoundId(UUID roundId, Pageable p);

    @EntityGraph(attributePaths = {"round", "team"})
    Page<RoundParticipant> findByTeamId(UUID teamId, Pageable p);

    @EntityGraph(attributePaths = {"round", "team"})
    Page<RoundParticipant> findByStatus(RoundParticipantStatus status, Pageable p);

    boolean existsByRoundIdAndTeamId(UUID roundId, UUID teamId);

    boolean existsByRoundId(UUID roundId);

    boolean existsByRoundLogicalRoundIdAndTeamId(UUID logicalRoundId, UUID teamId);

    boolean existsByTeamId(UUID teamId);

    long countByRoundId(UUID roundId);

    @EntityGraph(attributePaths = {"round", "team"})
    Optional<RoundParticipant> findWithRelationsById(UUID id);
}
