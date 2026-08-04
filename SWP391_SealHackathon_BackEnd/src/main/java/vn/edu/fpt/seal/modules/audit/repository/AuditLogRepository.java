package vn.edu.fpt.seal.modules.audit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.common.enums.AuditAction;
import vn.edu.fpt.seal.modules.audit.entity.AuditLog;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository truy xuất nhật ký kiểm toán (AuditLog).
 * Hỗ trợ phân trang và lọc theo user/team/incident/action.
 * Dùng @EntityGraph để nạp sẵn quan hệ, tránh N+1 query.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    /**
     * Lọc nhật ký theo người dùng, có phân trang.
     */
    @EntityGraph(attributePaths = {"user", "team", "incident"})
    Page<AuditLog> findByUserId(UUID userId, Pageable p);

    /**
     * Lọc nhật ký theo đội, có phân trang.
     */
    @EntityGraph(attributePaths = {"user", "team", "incident"})
    Page<AuditLog> findByTeamId(UUID teamId, Pageable p);

    /**
     * Lọc nhật ký theo sự cố, có phân trang.
     */
    @EntityGraph(attributePaths = {"user", "team", "incident"})
    Page<AuditLog> findByIncidentId(UUID incidentId, Pageable p);

    /**
     * Lọc nhật ký theo loại hành động, có phân trang.
     */
    @EntityGraph(attributePaths = {"user", "team", "incident"})
    Page<AuditLog> findByAction(AuditAction action, Pageable p);

    /**
     * Lấy một bản ghi kèm quan hệ đã nạp sẵn.
     */
    @EntityGraph(attributePaths = {"user", "team", "incident"})
    Optional<AuditLog> findWithRelationsById(UUID id);
}
