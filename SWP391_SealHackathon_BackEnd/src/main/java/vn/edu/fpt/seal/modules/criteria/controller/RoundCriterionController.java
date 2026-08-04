package vn.edu.fpt.seal.modules.criteria.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.seal.modules.criteria.dto.CreateRoundCriterionRequest;
import vn.edu.fpt.seal.modules.criteria.dto.RoundCriterionResponse;
import vn.edu.fpt.seal.modules.criteria.dto.UpdateRoundCriterionRequest;
import vn.edu.fpt.seal.modules.criteria.service.RoundCriterionService;

import java.util.UUID;

@RestController
@RequestMapping("/round-criteria")
@RequiredArgsConstructor
@Tag(name = "Round Criteria")
public class RoundCriterionController {
    private final RoundCriterionService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List criteria by round")
    public ResponseEntity<Page<RoundCriterionResponse>> list(@RequestParam UUID roundId, @RequestParam(required = false) UUID eventId, @RequestParam(required = false) UUID trackId, Pageable pageable) {
        return ResponseEntity.ok(service.list(eventId, roundId, trackId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RoundCriterionResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<RoundCriterionResponse> create(@Valid @RequestBody CreateRoundCriterionRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<RoundCriterionResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateRoundCriterionRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
