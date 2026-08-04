package vn.edu.fpt.seal.modules.incident.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.incident.entity.IncidentAction;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentActionRepository extends JpaRepository<IncidentAction, UUID> {
    @EntityGraph(attributePaths = {"incident", "actionBy"})
    List<IncidentAction> findByIncidentIdOrderByCreatedAtAsc(UUID incidentId);
}
