package vn.edu.fpt.seal.modules.incident.repository;
import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.*; import org.springframework.stereotype.Repository; import vn.edu.fpt.seal.common.enums.IncidentStatus; import vn.edu.fpt.seal.modules.incident.entity.IncidentReport; import java.util.*;
@Repository public interface IncidentReportRepository extends JpaRepository<IncidentReport, UUID>{
 Page<IncidentReport> findByEventId(UUID eventId, Pageable pageable); Page<IncidentReport> findByReporterId(UUID reporterId, Pageable pageable); Page<IncidentReport> findByStatus(IncidentStatus status, Pageable pageable);
 @EntityGraph(attributePaths={"event","track","round","team","submission","reporter","assignedCoordinator"}) Optional<IncidentReport> findWithRelationsById(UUID id);
}
