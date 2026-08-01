package vn.edu.fpt.seal.modules.score.repository;

import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable; import org.springframework.data.jpa.repository.EntityGraph; import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.data.jpa.repository.Query; import org.springframework.data.repository.query.Param; import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.score.entity.Score; import java.math.BigDecimal; import java.util.*;
@Repository public interface ScoreRepository extends JpaRepository<Score, UUID> {
    Page<Score> findBySubmissionId(UUID submissionId, Pageable pageable); Page<Score> findByJudgeId(UUID judgeId, Pageable pageable);
    Optional<Score> findBySubmissionIdAndJudgeIdAndCriterionId(UUID submissionId, UUID judgeId, UUID criterionId);
    @EntityGraph(attributePaths={"submission","submission.round","submission.team","judge","criterion","criterion.round"}) Optional<Score> findWithRelationsById(UUID id);

    /**
     * Every raw score in a round, flattened with team/criterion/judge ids, used
     * to build the inter-judge variance dashboard (#13) and the anonymized
     * dataset export (#12). Ordered for stable output.
     */
    @Query("""
            select
                sub.team.id as teamId,
                sub.team.name as teamName,
                sub.team.track.id as trackId,
                sub.team.track.name as trackName,
                sub.id as submissionId,
                cr.id as criterionId,
                cr.name as criterionName,
                cr.weight as criterionWeight,
                sc.judge.id as judgeId,
                sc.judge.fullName as judgeName,
                sc.score as score,
                sc.weightedScore as weightedScore
            from Score sc
            join sc.submission sub
            join sc.criterion cr
            where sub.round.id = :roundId
              and (:trackId is null or sub.team.track.id = :trackId)
            order by sub.team.name asc, cr.name asc, sc.judge.id asc
            """)
    List<RoundScoreDetailRow> findRoundScoreDetails(@Param("roundId") UUID roundId, @Param("trackId") UUID trackId);

    interface RoundScoreDetailRow {
        UUID getTeamId();
        String getTeamName();
        UUID getTrackId();
        String getTrackName();
        UUID getSubmissionId();
        UUID getCriterionId();
        String getCriterionName();
        BigDecimal getCriterionWeight();
        UUID getJudgeId();
        String getJudgeName();
        BigDecimal getScore();
        BigDecimal getWeightedScore();
    }
}
