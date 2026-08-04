package vn.edu.fpt.seal.modules.appeal.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.seal.modules.appeal.dto.AppealDtos;
import vn.edu.fpt.seal.modules.appeal.service.AppealService;

import java.util.List;
import java.util.UUID;

/**
 * REST controller cho luồng khiếu nại kết quả.
 * Định tuyến các endpoint tạo, tra cứu, phản hồi và giải quyết khiếu nại.
 * Kiểm soát quyền truy cập qua @PreAuthorize theo từng endpoint.
 */
@RestController
@RequestMapping("/appeals")
@RequiredArgsConstructor
public class AppealController {
    private final AppealService service;

    /**
     * Tạo khiếu nại mới — yêu cầu đăng nhập; chỉ đội trưởng mới khiếu nại được (kiểm tra trong service).
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AppealDtos.Response> create(@Valid @RequestBody AppealDtos.Create r, Authentication a) {
        return ResponseEntity.ok(service.create(r, a));
    }

    /**
     * Liệt kê khiếu nại theo sự kiện, lọc theo trạng thái — chỉ COORDINATOR.
     */
    @GetMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<List<AppealDtos.Response>> list(@RequestParam UUID eventId, @RequestParam(required = false) String status) {
        return ResponseEntity.ok(service.list(eventId, status));
    }

    /**
     * Lấy khiếu nại của một đội — yêu cầu đăng nhập; service kiểm tra phạm vi truy cập.
     */
    @GetMapping("/teams/{teamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AppealDtos.Response>> team(@PathVariable UUID teamId, Authentication a) {
        return ResponseEntity.ok(service.team(teamId, a));
    }

    /**
     * Lấy chi tiết một khiếu nại theo id — yêu cầu đăng nhập; service kiểm tra phạm vi.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AppealDtos.Response> get(@PathVariable UUID id, Authentication a) {
        return ResponseEntity.ok(service.get(id, a));
    }

    /**
     * Ghi phản hồi / yêu cầu làm rõ cho khiếu nại — chỉ COORDINATOR.
     */
    @PostMapping("/{id}/respond")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<AppealDtos.Response> respond(@PathVariable UUID id, @Valid @RequestBody AppealDtos.Respond r) {
        return ResponseEntity.ok(service.respond(id, r));
    }

    /**
     * Giải quyết (chấp nhận/từ chối) khiếu nại — chỉ COORDINATOR.
     */
    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<AppealDtos.Response> resolve(@PathVariable UUID id, @Valid @RequestBody AppealDtos.Resolve r, Authentication a) {
        return ResponseEntity.ok(service.resolve(id, r, a));
    }
}
