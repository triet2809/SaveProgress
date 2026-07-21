package vn.edu.fpt.seal.modules.incident.repository;
import org.springframework.data.jpa.repository.*; import org.springframework.stereotype.Repository; import vn.edu.fpt.seal.modules.incident.entity.IncidentAction; import java.util.*;
@Repository public interface IncidentActionRepository extends JpaRepository<IncidentAction, UUID>{ @EntityGraph(attributePaths={"incident","actionBy"}) List<IncidentAction> findByIncidentIdOrderByCreatedAtAsc(UUID incidentId); }
