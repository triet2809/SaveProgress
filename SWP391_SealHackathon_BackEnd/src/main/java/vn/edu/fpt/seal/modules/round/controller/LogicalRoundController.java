package vn.edu.fpt.seal.modules.round.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.seal.modules.round.dto.LogicalRoundProgressionDtos.BalancePreview;
import vn.edu.fpt.seal.modules.round.dto.LogicalRoundProgressionDtos.BalanceRequest;
import vn.edu.fpt.seal.modules.round.dto.LogicalRoundProgressionDtos.ManualAssignmentRequest;
import vn.edu.fpt.seal.modules.round.dto.LogicalRoundProgressionDtos.PromotedTeam;
import vn.edu.fpt.seal.modules.round.dto.LogicalRoundResponse;
import vn.edu.fpt.seal.modules.round.dto.UpdateLogicalRoundRequest;
import vn.edu.fpt.seal.modules.round.service.LogicalRoundIntegrityService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/logical-rounds")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COORDINATOR')")
@Tag(name = "Logical Rounds", description = "Event-level round metadata and progression assignments")
public class LogicalRoundController {
    private final LogicalRoundIntegrityService service;

    @GetMapping
    @Operation(summary = "List logical rounds by event (coordinator only)")
    public ResponseEntity<List<LogicalRoundResponse>> list(@RequestParam UUID eventId) {
        return ResponseEntity.ok(service.listByEvent(eventId));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update shared logical-round metadata")
    public ResponseEntity<LogicalRoundResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateLogicalRoundRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an empty, unlocked logical round")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteLogical(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/promoted-unassigned")
    @Operation(summary = "List promoted teams not yet assigned to this logical round")
    public ResponseEntity<List<PromotedTeam>> promotedUnassigned(@PathVariable UUID id) {
        return ResponseEntity.ok(service.promotedUnassigned(id));
    }

    @PostMapping("/{id}/assignments")
    @Operation(summary = "Assign promoted teams manually to round track executions")
    public ResponseEntity<Void> assign(
            @PathVariable UUID id, @Valid @RequestBody ManualAssignmentRequest request) {
        service.assignManual(id, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/assignments/balance-preview")
    @Operation(summary = "Preview deterministic balanced assignment of promoted teams")
    public ResponseEntity<BalancePreview> preview(
            @PathVariable UUID id, @RequestBody(required = false) BalanceRequest request) {
        return ResponseEntity.ok(service.previewBalanced(id, request));
    }

    @PostMapping("/{id}/assignments/balance-apply")
    @Operation(summary = "Apply deterministic balanced assignment of promoted teams")
    public ResponseEntity<BalancePreview> apply(
            @PathVariable UUID id, @RequestBody(required = false) BalanceRequest request) {
        return ResponseEntity.ok(service.applyBalanced(id, request));
    }
}
