package vn.edu.fpt.seal.modules.casework.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.seal.modules.casework.dto.*;
import vn.edu.fpt.seal.modules.casework.service.CaseService;
import java.util.UUID;

@RestController @RequestMapping("/cases") @RequiredArgsConstructor
public class CaseController {
    private final CaseService service;
    @GetMapping @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<CaseResponse>> list(@RequestParam(required=false) UUID eventId, @RequestParam(required=false) UUID reporterId, Pageable p, Authentication a) { return ResponseEntity.ok(service.list(eventId, reporterId, p, a)); }
    @GetMapping("/{id}") @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CaseResponse> get(@PathVariable String id, Authentication a) { return ResponseEntity.ok(service.get(parse(id), a)); }
    @PostMapping @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CaseResponse> create(@Valid @RequestBody CreateCaseRequest r, Authentication a) { return ResponseEntity.ok(service.create(r, a)); }
    @PatchMapping("/{id}/status") @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<CaseResponse> status(@PathVariable String id, @Valid @RequestBody UpdateCaseStatusRequest r, Authentication a) { return ResponseEntity.ok(service.updateStatus(parse(id), r, a)); }
    private UUID parse(String id) { try { return UUID.fromString(id.startsWith("INC-") ? id.substring(4) : id); } catch (IllegalArgumentException e) { throw vn.edu.fpt.seal.common.exception.ApiException.badRequest("Invalid case id"); } }
}
