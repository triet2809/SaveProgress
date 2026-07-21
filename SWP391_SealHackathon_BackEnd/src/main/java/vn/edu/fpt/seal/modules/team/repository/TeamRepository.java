package vn.edu.fpt.seal.modules.team.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.seal.modules.team.entity.Team;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    Page<Team> findByTrackId(UUID trackId, Pageable pageable);
    @EntityGraph(attributePaths = {"event", "track"})
    @Query("select t from Team t where t.event.id = :eventId")
    Page<Team> findByTrackEventId(@Param("eventId") UUID eventId, Pageable pageable);
    @EntityGraph(attributePaths = {"event", "track"})
    @Query("select t from Team t where t.event.id = :eventId and t.track.id = :trackId")
    Page<Team> findByTrackEventIdAndTrackId(@Param("eventId") UUID eventId,
                                            @Param("trackId") UUID trackId, Pageable pageable);

    boolean existsByTrackIdAndNameIgnoreCase(UUID trackId, String name);

    boolean existsByEventIdAndTrackIsNullAndNameIgnoreCase(UUID eventId, String name);

    @EntityGraph(attributePaths = {"event", "track"})
    Optional<Team> findByInviteCodeIgnoreCase(String inviteCode);

    boolean existsByInviteCode(String inviteCode);

    @EntityGraph(attributePaths = {"event", "track"})
    Optional<Team> findWithTrackById(UUID id);

    @Query("select count(t) from Team t where t.event.id = :eventId")
    long countByTrackEventId(@Param("eventId") UUID eventId);

    @EntityGraph(attributePaths = {"event", "track"})
    @Query("select t from Team t where t.event.id = :eventId")
    List<Team> findByTrackEventId(@Param("eventId") UUID eventId);

    @EntityGraph(attributePaths = {"event", "track", "teamProfile", "sourceTeam"})
    List<Team> findByTeamProfileIdIn(Collection<UUID> profileIds);

    @Query("select (count(t) > 0) from Team t where t.teamProfile.id = :profileId and t.event.id = :eventId")
    boolean existsByTeamProfileIdAndTrackEventId(@Param("profileId") UUID profileId,
                                                  @Param("eventId") UUID eventId);

    long countByTrackId(UUID trackId);
}
