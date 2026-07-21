package vn.edu.fpt.seal.modules.submission.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.submission.entity.Submission;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
    Page<Submission> findByRoundId(UUID roundId, Pageable pageable);
    Page<Submission> findByTeamId(UUID teamId, Pageable pageable);
    Optional<Submission> findByRoundIdAndTeamId(UUID roundId, UUID teamId);
    boolean existsByRoundIdAndTeamId(UUID roundId, UUID teamId);
    boolean existsByRoundId(UUID roundId);
    boolean existsByTeamId(UUID teamId);
    @EntityGraph(attributePaths = {"round", "round.track", "round.track.event", "team", "team.track"})
    @Query("""
            select s from Submission s
            join s.round r
            join s.team t
            join t.track tr
            where (:roundId is null or r.id = :roundId)
              and (:teamId is null or t.id = :teamId)
              and (:trackId is null or tr.id = :trackId)
            """)
    Page<Submission> search(@Param("roundId") UUID roundId, @Param("teamId") UUID teamId, @Param("trackId") UUID trackId, Pageable pageable);

    @EntityGraph(attributePaths = {"round", "round.track", "round.track.event", "team", "team.track"})
    @Query("""
            select s from Submission s join s.round r join r.track tr
            where tr.event.id = :eventId
              and (:roundId is null or r.id = :roundId)
              and (:trackId is null or tr.id = :trackId)
            """)
    Page<Submission> searchByEvent(@Param("eventId") UUID eventId, @Param("roundId") UUID roundId,
                                   @Param("trackId") UUID trackId, Pageable pageable);

    @EntityGraph(attributePaths = {"round", "round.track", "round.track.event", "team", "team.track"})
    Optional<Submission> findWithRelationsById(UUID id);
}
