package vn.edu.fpt.seal.modules.staff.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.seal.modules.staff.dto.EventStaffResponse;
import vn.edu.fpt.seal.modules.staff.dto.InviteStaffRequest;
import vn.edu.fpt.seal.modules.staff.dto.UpdateStaffAssignmentsRequest;
import vn.edu.fpt.seal.modules.staff.service.EventStaffService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/events/{eventId}/staff")
@RequiredArgsConstructor
public class EventStaffController {
    private final EventStaffService service;

    @GetMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<List<EventStaffResponse>> list(@PathVariable UUID eventId) {
        return ResponseEntity.ok(service.list(eventId));
    }

    @PostMapping("/invite")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<EventStaffResponse> invite(@PathVariable UUID eventId, @Valid @RequestBody InviteStaffRequest request) {
        return ResponseEntity.ok(service.invite(eventId, request));
    }

    @PutMapping("/{userId}/assignments")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<EventStaffResponse> update(@PathVariable UUID eventId, @PathVariable UUID userId,
                                                     @Valid @RequestBody UpdateStaffAssignmentsRequest request) {
        return ResponseEntity.ok(service.update(eventId, userId, request));
    }

    @DeleteMapping("/{userId}/assignments/{assignmentType}")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<Void> remove(@PathVariable UUID eventId, @PathVariable UUID userId,
                                       @PathVariable String assignmentType) {
        service.remove(eventId, userId, assignmentType);
        return ResponseEntity.noContent().build();
    }
}
