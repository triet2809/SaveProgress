package vn.edu.fpt.seal.modules.incident.repository;
import org.springframework.data.jpa.repository.*; import org.springframework.stereotype.Repository; import vn.edu.fpt.seal.modules.incident.entity.IncidentEvidence; import java.util.*;
@Repository public interface IncidentEvidenceRepository extends JpaRepository<IncidentEvidence, UUID>{ @EntityGraph(attributePaths={"incident","uploadedBy"}) List<IncidentEvidence> findByIncidentIdOrderByCreatedAtAsc(UUID incidentId); }
