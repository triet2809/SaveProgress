package vn.edu.fpt.seal.modules.seeding.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.seal.common.enums.EventStatus;
import vn.edu.fpt.seal.common.enums.TeamStatus;
import vn.edu.fpt.seal.modules.seeding.entity.EventTeamFinish;

import java.util.*;

public interface EventTeamFinishRepository extends JpaRepository<EventTeamFinish, UUID> {
    boolean existsByTeamId(UUID teamId);
    Optional<EventTeamFinish> findByTeamId(UUID teamId);
    long countByTrackId(UUID trackId);
    long countByFinalRoundId(UUID finalRoundId);

    @EntityGraph(attributePaths = {"event", "track", "team", "teamProfile", "finalRound", "resultVersion"})
    List<EventTeamFinish> findByEventId(UUID eventId);

    @EntityGraph(attributePaths = {"event", "track", "team", "teamProfile", "finalRound", "resultVersion"})
    List<EventTeamFinish> findByTeamProfileIdIn(Collection<UUID> profileIds);

    @Query("""
            select count(distinct f.event.id)
              from EventTeamFinish f
             where f.teamProfile.id = :profileId
               and f.event.status = :eventStatus
               and f.completionStatus = 'completed'
               and f.team.status <> :disqualified
            """)
    long countDistinctQualifyingEvents(@Param("profileId") UUID profileId,
                                       @Param("eventStatus") EventStatus eventStatus,
                                       @Param("disqualified") TeamStatus disqualified);
}
