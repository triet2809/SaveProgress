package vn.edu.fpt.seal.modules.round.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.seal.modules.round.dto.*;
import vn.edu.fpt.seal.modules.round.service.LogicalRoundIntegrityService;
import vn.edu.fpt.seal.modules.round.service.RoundService;

import java.util.UUID;

@RestController
@RequestMapping("/rounds")
@RequiredArgsConstructor
@Tag(name = "Rounds", description = "Competition rounds within a track")
public class RoundController {

    private final RoundService roundService;
    private final LogicalRoundIntegrityService logicalRoundIntegrityService;


    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List rounds by track")
    public ResponseEntity<Page<RoundResponse>> listByTrack(
            @RequestParam(required = false) UUID trackId,
            @RequestParam(required = false) UUID eventId,
            Pageable pageable) {
        return ResponseEntity.ok(eventId != null ? roundService.listByEvent(eventId, pageable) : roundService.listByTrack(trackId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get round by id")
    public ResponseEntity<RoundResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(roundService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Create round (coordinator only)")
    public ResponseEntity<RoundResponse> create(@Valid @RequestBody CreateRoundRequest req) {
        return ResponseEntity.ok(roundService.create(req));
    }

    @PostMapping("/logical")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Create one logical round across selected event tracks")
    public ResponseEntity<LogicalRoundResponse> createLogical(@Valid @RequestBody CreateLogicalRoundRequest req) {
        return ResponseEntity.ok(roundService.createLogical(req));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Update round (coordinator only)")
    public ResponseEntity<RoundResponse> update(@PathVariable UUID id,
                                                @Valid @RequestBody UpdateRoundRequest req) {
        return ResponseEntity.ok(roundService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Delete an empty, unlocked round track execution")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        logicalRoundIntegrityService.deleteExecution(id);
        return ResponseEntity.noContent().build();
    }
}
