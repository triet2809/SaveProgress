package vn.edu.fpt.seal.modules.seeding.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.seal.modules.seeding.dto.SeedingDtos;
import vn.edu.fpt.seal.modules.seeding.service.SeedingService;

import java.util.*;

@RestController
@RequestMapping("/events/{eventId}")
@RequiredArgsConstructor
public class SeedingController {
    private final SeedingService service;

    @PostMapping("/finalize-results")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<SeedingDtos.FinalizationResponse> finalizeResults(
            @PathVariable UUID eventId, Authentication authentication) {
        return ResponseEntity.ok(service.finalizeResults(eventId, authentication));
    }

    @GetMapping("/seed-candidates")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<SeedingDtos.CandidateResponse> candidates(
            @PathVariable UUID eventId, @RequestParam(required = false) UUID trackId) {
        return ResponseEntity.ok(service.candidates(eventId, trackId));
    }

    @GetMapping("/seeds")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<List<SeedingDtos.Assignment>> seeds(@PathVariable UUID eventId) {
        return ResponseEntity.ok(service.seeds(eventId));
    }

    @PutMapping("/teams/{teamId}/seed")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<SeedingDtos.Assignment> decide(
            @PathVariable UUID eventId, @PathVariable UUID teamId,
            @Valid @RequestBody SeedingDtos.SeedDecisionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(service.decide(eventId, teamId, request, authentication));
    }

    @DeleteMapping("/teams/{teamId}/seed")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<Void> remove(@PathVariable UUID eventId, @PathVariable UUID teamId,
                                       Authentication authentication) {
        service.remove(eventId, teamId, authentication);
        return ResponseEntity.noContent().build();
    }
}
