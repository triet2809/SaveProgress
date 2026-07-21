package vn.edu.fpt.seal.modules.ranking.repository;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.ranking.entity.RoundRanking;

import java.math.BigDecimal;
import java.util.*;

@Repository
public interface RoundRankingRepository extends JpaRepository<RoundRanking, UUID> {
    @EntityGraph(attributePaths = {"round", "round.track", "team", "tieBreakerCriterion"})
    Page<RoundRanking> findByRoundId(UUID roundId, Pageable pageable);

    Optional<RoundRanking> findByRoundIdAndTeamId(UUID roundId, UUID teamId);
    boolean existsByTeamId(UUID teamId);
    boolean existsByRoundId(UUID roundId);

    void deleteByRoundId(UUID roundId);

    @Query("""
            select
                sub.team.id as teamId,
                coalesce(sum(sc.weightedScore), 0) as totalScore,
                sub.team.name as teamName
            from Submission sub
            left join Score sc on sc.submission.id = sub.id
            where sub.round.id = :roundId
            group by sub.team.id, sub.team.name
            order by coalesce(sum(sc.weightedScore), 0) desc, sub.team.name asc
            """)
    List<RoundScoreRow> calculateRows(@Param("roundId") UUID roundId);

    /**
     * Per-team, per-criterion aggregated weighted score for a round, used to
     * break ties (requirement #8): when two teams share the same total, the
     * team with the higher weighted score on the highest-weight criterion wins.
     * Ordered by criterion weight desc so the first matching criterion a caller
     * iterates is the most important one.
     */
    @Query("""
            select
                sub.team.id as teamId,
                cr.id as criterionId,
                cr.name as criterionName,
                cr.weight as criterionWeight,
                coalesce(sum(sc.weightedScore), 0) as criterionScore
            from Submission sub
            join Score sc on sc.submission.id = sub.id
            join sc.criterion cr
            where sub.round.id = :roundId
            group by sub.team.id, cr.id, cr.name, cr.weight
            order by cr.weight desc, cr.name asc
            """)
    List<TeamCriterionScoreRow> criterionScores(@Param("roundId") UUID roundId);

    interface RoundScoreRow {
        UUID getTeamId();
        BigDecimal getTotalScore();
        String getTeamName();
    }

    interface TeamCriterionScoreRow {
        UUID getTeamId();
        UUID getCriterionId();
        String getCriterionName();
        BigDecimal getCriterionWeight();
        BigDecimal getCriterionScore();
    }
}
