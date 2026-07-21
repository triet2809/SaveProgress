package vn.edu.fpt.seal.modules.seeding.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.seal.modules.seeding.entity.EventSeedAssignment;

import java.util.*;

public interface EventSeedAssignmentRepository extends JpaRepository<EventSeedAssignment, UUID> {
    @EntityGraph(attributePaths = {"event", "track", "team", "teamProfile", "candidateSourceFinish", "assignedBy"})
    List<EventSeedAssignment> findByEventId(UUID eventId);

    @EntityGraph(attributePaths = {"event", "track", "team", "teamProfile", "candidateSourceFinish", "assignedBy"})
    Optional<EventSeedAssignment> findByEventIdAndTeamIdAndCompetitionStage(
            UUID eventId, UUID teamId, String competitionStage);
    boolean existsByEventIdAndTeamId(UUID eventId, UUID teamId);
}
