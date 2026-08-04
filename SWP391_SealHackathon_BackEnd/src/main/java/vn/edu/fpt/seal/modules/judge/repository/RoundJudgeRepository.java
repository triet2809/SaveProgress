package vn.edu.fpt.seal.modules.judge.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.judge.entity.RoundJudge;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoundJudgeRepository extends JpaRepository<RoundJudge, UUID> {
    @EntityGraph(attributePaths = {"round", "user"})
    Page<RoundJudge> findByRoundId(UUID roundId, Pageable pageable);

    @EntityGraph(attributePaths = {"round", "user"})
    Page<RoundJudge> findByUserId(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"round", "round.track", "round.track.event", "user"})
    Page<RoundJudge> findByRoundTrackEventId(UUID eventId, Pageable pageable);

    @EntityGraph(attributePaths = {"round", "round.track", "round.track.event", "user"})
    Page<RoundJudge> findByRoundTrackEventIdAndRoundId(UUID eventId, UUID roundId, Pageable pageable);

    @EntityGraph(attributePaths = {"round", "round.track", "round.track.event", "user"})
    Page<RoundJudge> findByRoundTrackEventIdAndUserId(UUID eventId, UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"round", "round.track", "round.track.event", "user"})
    List<RoundJudge> findAllByRoundTrackEventId(UUID eventId);

    @EntityGraph(attributePaths = {"round", "user"})
    Optional<RoundJudge> findWithRelationsById(UUID id);

    boolean existsByRoundIdAndUserId(UUID roundId, UUID userId);

    boolean existsByRoundId(UUID roundId);

    Optional<RoundJudge> findByRoundIdAndUserId(UUID roundId, UUID userId);

    @Query(value = """
            select rj.id as roundJudgeId, rj.user_id as judgeId, e.id as eventId, e.title as eventName,
                   tr.id as trackId, tr.name as trackName, r.id as roundId, r.name as roundName,
                   t.id as teamId, t.name as teamName, s.id as submissionId, s.repo_url as repoUrl,
                   s.slide_url as presentationUrl, s.demo_url as demoUrl, s.status as status,
                   s.review_status as reviewStatus, s.submitted_at as submittedAt
            from round_judges rj
            join rounds r on r.id = rj.round_id
            join tracks tr on tr.id = r.track_id
            join events e on e.id = tr.event_id
            join submissions s on s.round_id = r.id
            join teams t on t.id = s.team_id
            where rj.user_id = :judgeId
              and (:eventId is null or e.id = :eventId)
              and (:roundId is null or r.id = :roundId)
              and (:trackId is null or tr.id = :trackId)
            order by r.submission_deadline asc, t.name asc
            """, nativeQuery = true)
    List<JudgeSubmissionRow> findSubmissionRowsForJudge(@Param("judgeId") UUID judgeId, @Param("eventId") UUID eventId,
                                                        @Param("roundId") UUID roundId, @Param("trackId") UUID trackId);

    interface JudgeSubmissionRow {
        UUID getRoundJudgeId();

        UUID getJudgeId();

        UUID getEventId();

        String getEventName();

        UUID getTrackId();

        String getTrackName();

        UUID getRoundId();

        String getRoundName();

        UUID getTeamId();

        String getTeamName();

        UUID getSubmissionId();

        String getRepoUrl();

        String getPresentationUrl();

        String getDemoUrl();

        String getStatus();

        String getReviewStatus();

        LocalDateTime getSubmittedAt();
    }
}
