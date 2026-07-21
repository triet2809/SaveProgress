package vn.edu.fpt.seal.modules.team.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.common.enums.TeamMemberRole;
import vn.edu.fpt.seal.modules.team.entity.TeamMember;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    @EntityGraph(attributePaths = {"user"})
    List<TeamMember> findByTeamIdOrderByRoleAscJoinedAtAsc(UUID teamId);

    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);

    boolean existsByUserIdAndTeamTrackId(UUID userId, UUID trackId);

    long countByTeamId(UUID teamId);

    boolean existsByTeamIdAndRole(UUID teamId, TeamMemberRole role);

    Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId);

    @EntityGraph(attributePaths = {"team", "team.teamProfile", "team.event", "team.track", "user"})
    List<TeamMember> findByUserIdOrderByJoinedAtDesc(UUID userId);

    @EntityGraph(attributePaths = {"team", "team.event", "team.track", "user"})
    List<TeamMember> findByTeamIdIn(Collection<UUID> teamIds);

    @Query(value = """
        select tm.* from public.team_members tm
        join public.teams team on team.id = tm.team_id
        where tm.user_id in (:userIds)
          and team.event_id = :eventId
          and team.status = 'active'::public.team_status
        """, nativeQuery = true)
    List<TeamMember> findActiveRegistrationsInEvent(@Param("userIds") Collection<UUID> userIds,
                                                     @Param("eventId") UUID eventId);

    default boolean existsActiveRegistrationInEvent(UUID userId, UUID eventId) {
        return !findActiveRegistrationsInEvent(List.of(userId), eventId).isEmpty();
    }
}
