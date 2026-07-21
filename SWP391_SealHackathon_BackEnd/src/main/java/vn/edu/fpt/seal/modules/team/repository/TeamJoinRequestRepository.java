package vn.edu.fpt.seal.modules.team.repository;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.team.entity.TeamJoinRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamJoinRequestRepository extends JpaRepository<TeamJoinRequest, UUID> {

    @EntityGraph(attributePaths = {"team", "team.track", "user"})
    List<TeamJoinRequest> findByTeamIdOrderByCreatedAtDesc(UUID teamId);

    @EntityGraph(attributePaths = {"team", "team.track", "user"})
    List<TeamJoinRequest> findByTeamIdAndStatusOrderByCreatedAtDesc(UUID teamId, String status);

    @EntityGraph(attributePaths = {"team", "team.track", "user"})
    List<TeamJoinRequest> findByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByTeamIdAndUserIdAndStatus(UUID teamId, UUID userId, String status);

    @EntityGraph(attributePaths = {"team", "team.track", "team.track.event", "user"})
    Optional<TeamJoinRequest> findWithRelationsById(UUID id);
}
