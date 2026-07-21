package vn.edu.fpt.seal.modules.team.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.team.entity.Team;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    Page<Team> findByTrackId(UUID trackId, Pageable pageable);
    @EntityGraph(attributePaths = {"track", "track.event"})
    Page<Team> findByTrackEventId(UUID eventId, Pageable pageable);
    @EntityGraph(attributePaths = {"track", "track.event"})
    Page<Team> findByTrackEventIdAndTrackId(UUID eventId, UUID trackId, Pageable pageable);

    boolean existsByTrackIdAndNameIgnoreCase(UUID trackId, String name);

    @EntityGraph(attributePaths = {"track", "track.event"})
    Optional<Team> findByInviteCodeIgnoreCase(String inviteCode);

    boolean existsByInviteCode(String inviteCode);

    @EntityGraph(attributePaths = {"track", "track.event"})
    Optional<Team> findWithTrackById(UUID id);

    long countByTrackEventId(UUID eventId);

    @EntityGraph(attributePaths = {"track", "track.event"})
    List<Team> findByTrackEventId(UUID eventId);

    @EntityGraph(attributePaths = {"track", "track.event", "teamProfile", "sourceTeam"})
    List<Team> findByTeamProfileIdIn(Collection<UUID> profileIds);

    boolean existsByTeamProfileIdAndTrackEventId(UUID profileId, UUID eventId);

    long countByTrackId(UUID trackId);
}
