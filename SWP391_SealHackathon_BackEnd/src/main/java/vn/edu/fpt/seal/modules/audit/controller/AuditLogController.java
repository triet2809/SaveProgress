package vn.edu.fpt.seal.modules.audit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.seal.common.enums.AuditAction;
import vn.edu.fpt.seal.modules.audit.dto.AuditLogResponse;
import vn.edu.fpt.seal.modules.audit.dto.CreateAuditLogRequest;
import vn.edu.fpt.seal.modules.audit.service.AuditLogService;

import java.util.UUID;

/**
 * REST controller cho nhật ký kiểm toán (audit logs).
 * Tất cả endpoint chỉ dành cho COORDINATOR vì dữ liệu nhạy cảm/truy vết.
 */
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {
    private final AuditLogService service;

    /**
     * Liệt kê nhật ký có phân trang, lọc tuỳ chọn theo user/team/incident/action.
     */
    @GetMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<Page<AuditLogResponse>> list(@RequestParam(required = false) UUID userId, @RequestParam(required = false) UUID teamId, @RequestParam(required = false) UUID incidentId, @RequestParam(required = false) AuditAction action, Pageable p) {
        return ResponseEntity.ok(service.list(userId, teamId, incidentId, action, p));
    }

    /**
     * Lấy chi tiết một bản ghi nhật ký theo id.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<AuditLogResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    /**
     * Tạo thủ công một bản ghi nhật ký.
     */
    @PostMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<AuditLogResponse> create(@Valid @RequestBody CreateAuditLogRequest r) {
        return ResponseEntity.ok(service.create(r));
    }
}
