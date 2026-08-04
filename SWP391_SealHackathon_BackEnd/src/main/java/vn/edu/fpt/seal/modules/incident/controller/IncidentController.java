package vn.edu.fpt.seal.modules.incident.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.seal.common.enums.IncidentStatus;
import vn.edu.fpt.seal.modules.incident.dto.*;
import vn.edu.fpt.seal.modules.incident.service.IncidentService;

import java.util.UUID;

@RestController
@RequestMapping("/incidents")
@RequiredArgsConstructor
@Tag(name = "Incidents")
public class IncidentController {
    private final IncidentService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<IncidentResponse>> list(@RequestParam(required = false) UUID eventId, @RequestParam(required = false) UUID reporterId, @RequestParam(required = false) IncidentStatus status, Pageable p, Authentication auth) {
        return ResponseEntity.ok(service.list(eventId, reporterId, status, p, auth));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<IncidentResponse> get(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(service.get(id, auth));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<IncidentResponse> create(@Valid @RequestBody CreateIncidentRequest req, Authentication auth) {
        return ResponseEntity.ok(service.create(req, auth));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<IncidentResponse> updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateIncidentStatusRequest req, Authentication auth) {
        return ResponseEntity.ok(service.updateStatus(id, req, auth));
    }

    @PostMapping("/{id}/evidences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<IncidentResponse> addEvidence(@PathVariable UUID id, @Valid @RequestBody AddIncidentEvidenceRequest req, Authentication auth) {
        return ResponseEntity.ok(service.addEvidence(id, req, auth));
    }

    @PostMapping("/{id}/actions")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<IncidentResponse> addAction(@PathVariable UUID id, @Valid @RequestBody AddIncidentActionRequest req, Authentication auth) {
        return ResponseEntity.ok(service.addAction(id, req, auth));
    }
}
