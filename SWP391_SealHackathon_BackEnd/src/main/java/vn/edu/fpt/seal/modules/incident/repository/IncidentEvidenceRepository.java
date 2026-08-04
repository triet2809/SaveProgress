package vn.edu.fpt.seal.modules.incident.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.incident.entity.IncidentEvidence;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentEvidenceRepository extends JpaRepository<IncidentEvidence, UUID> {
    @EntityGraph(attributePaths = {"incident", "uploadedBy"})
    List<IncidentEvidence> findByIncidentIdOrderByCreatedAtAsc(UUID incidentId);
}
