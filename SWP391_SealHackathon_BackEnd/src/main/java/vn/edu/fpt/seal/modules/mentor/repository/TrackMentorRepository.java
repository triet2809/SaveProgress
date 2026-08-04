package vn.edu.fpt.seal.modules.mentor.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.common.enums.TeamStatus;
import vn.edu.fpt.seal.modules.mentor.entity.TrackMentor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrackMentorRepository extends JpaRepository<TrackMentor, UUID> {
    @EntityGraph(attributePaths = {"event", "track", "user"})
    Page<TrackMentor> findByTrackId(UUID trackId, Pageable pageable);

    @EntityGraph(attributePaths = {"event", "track", "user"})
    Page<TrackMentor> findByUserId(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"event", "track", "user"})
    Page<TrackMentor> findByEventId(UUID eventId, Pageable pageable);

    @EntityGraph(attributePaths = {"event", "track", "user"})
    Page<TrackMentor> findByEventIdAndTrackId(UUID eventId, UUID trackId, Pageable pageable);

    @EntityGraph(attributePaths = {"event", "track", "user"})
    Page<TrackMentor> findByEventIdAndUserId(UUID eventId, UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"event", "track", "user"})
    List<TrackMentor> findAllByEventId(UUID eventId);

    boolean existsByTrackIdAndUserId(UUID trackId, UUID userId);

    boolean existsByEventIdAndUserId(UUID eventId, UUID userId);

    Optional<TrackMentor> findByTrackIdAndUserId(UUID trackId, UUID userId);

    @Query(value = """
            select tm.id as trackMentorId, tm.user_id as mentorId, e.id as eventId, e.title as eventName,
                   t.id as trackId, t.name as trackName,
                   r.id as roundId, r.name as roundName, team.id as teamId, team.name as teamName, team.status as teamStatus
            from track_mentors tm
            join tracks t on t.id = tm.track_id
            join events e on e.id = tm.event_id
            left join rounds r on r.track_id = t.id and (:roundId is not null and r.id = :roundId)
            left join teams team on team.track_id = t.id
            where tm.user_id = :mentorId
              and (:eventId is null or tm.event_id = :eventId)
              and (:trackId is null or tm.track_id = :trackId)
              and (:roundId is null or exists (select 1 from rounds r2 where r2.id = :roundId and r2.track_id = t.id))
            order by t.name asc, team.name asc, r.sequence_number asc
            """, nativeQuery = true)
    List<MentorTeamRow> findTeamRowsForMentor(@Param("mentorId") UUID mentorId, @Param("eventId") UUID eventId,
                                              @Param("trackId") UUID trackId, @Param("roundId") UUID roundId);

    interface MentorTeamRow {
        UUID getTrackMentorId();

        UUID getMentorId();

        UUID getEventId();

        String getEventName();

        UUID getTrackId();

        String getTrackName();

        UUID getRoundId();

        String getRoundName();

        UUID getTeamId();

        String getTeamName();

        TeamStatus getTeamStatus();
    }
}
