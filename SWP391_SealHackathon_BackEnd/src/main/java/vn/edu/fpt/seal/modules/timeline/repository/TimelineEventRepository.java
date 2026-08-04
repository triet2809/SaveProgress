package vn.edu.fpt.seal.modules.timeline.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.timeline.TimelineEventType;
import vn.edu.fpt.seal.modules.timeline.TimelineScope;
import vn.edu.fpt.seal.modules.timeline.entity.TimelineEvent;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TimelineEventRepository extends JpaRepository<TimelineEvent, UUID> {
    @EntityGraph(attributePaths = {"event", "team", "round", "track"})
    @Query("""
            select t from TimelineEvent t
            where t.event.id = :eventId
              and (:roundId is null or t.round.id = :roundId)
              and (:trackId is null or t.track.id = :trackId)
              and (:eventType is null or t.eventType = :eventType)
              and (:scope is null or t.visibilityScope = :scope)
            """)
    Page<TimelineEvent> search(@Param("eventId") UUID eventId, @Param("roundId") UUID roundId,
                               @Param("trackId") UUID trackId, @Param("eventType") TimelineEventType eventType,
                               @Param("scope") TimelineScope scope, Pageable pageable);

    Optional<TimelineEvent> findByIdempotencyKey(String key);

    @EntityGraph(attributePaths = {"event", "team", "round", "track"})
    Page<TimelineEvent> findByEventId(UUID eventId, Pageable pageable);

    @EntityGraph(attributePaths = {"event", "team", "round", "track"})
    Page<TimelineEvent> findByEventIdAndVisibilityScope(UUID eventId, TimelineScope scope, Pageable pageable);

    @EntityGraph(attributePaths = {"event", "team", "round", "track"})
    Page<TimelineEvent> findByEventIdAndRoundId(UUID eventId, UUID roundId, Pageable pageable);

    @EntityGraph(attributePaths = {"event", "team", "round", "track"})
    Page<TimelineEvent> findByEventIdAndRoundIdAndVisibilityScope(UUID eventId, UUID roundId, TimelineScope scope, Pageable pageable);

    @EntityGraph(attributePaths = {"event", "team", "round", "track"})
    Page<TimelineEvent> findByEventIdAndTrackId(UUID eventId, UUID trackId, Pageable pageable);

    @EntityGraph(attributePaths = {"event", "team", "round", "track"})
    Page<TimelineEvent> findByEventIdAndTrackIdAndVisibilityScope(UUID eventId, UUID trackId, TimelineScope scope, Pageable pageable);

    @EntityGraph(attributePaths = {"event", "team", "round", "track"})
    Page<TimelineEvent> findByTeamId(UUID teamId, Pageable pageable);

    @EntityGraph(attributePaths = {"event", "team", "round", "track"})
    Optional<TimelineEvent> findWithRelationsById(UUID id);
}
