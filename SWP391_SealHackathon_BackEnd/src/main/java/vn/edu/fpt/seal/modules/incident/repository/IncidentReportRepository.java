package vn.edu.fpt.seal.modules.incident.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.common.enums.IncidentStatus;
import vn.edu.fpt.seal.modules.incident.entity.IncidentReport;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentReportRepository extends JpaRepository<IncidentReport, UUID> {
    Page<IncidentReport> findByEventId(UUID eventId, Pageable pageable);

    Page<IncidentReport> findByReporterId(UUID reporterId, Pageable pageable);

    Page<IncidentReport> findByStatus(IncidentStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"event", "track", "round", "team", "submission", "reporter", "assignedCoordinator"})
    Optional<IncidentReport> findWithRelationsById(UUID id);
}
