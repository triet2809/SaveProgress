package vn.edu.fpt.seal.modules.team.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.seal.modules.team.dto.*;
import vn.edu.fpt.seal.modules.team.service.TeamJoinRequestService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/join-requests")
@RequiredArgsConstructor
@Tag(name = "Team Join Requests")
public class TeamJoinRequestController {

    private final TeamJoinRequestService service;

    /** Student requests to join a team. */
    @PostMapping @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JoinRequestResponse> create(@Valid @RequestBody CreateJoinRequestRequest req, Authentication auth) {
        return ResponseEntity.ok(service.create(req, auth));
    }

    /** Leader/coordinator lists requests for a team (optional status filter). */
    @GetMapping @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<JoinRequestResponse>> listForTeam(@RequestParam UUID teamId, @RequestParam(required = false) String status, Authentication auth) {
        return ResponseEntity.ok(service.listForTeam(teamId, status, auth));
    }

    /** Current user lists their own join requests. */
    @GetMapping("/mine") @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<JoinRequestResponse>> listMine(Authentication auth) {
        return ResponseEntity.ok(service.listMine(auth));
    }

    @PostMapping("/{id}/accept") @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JoinRequestResponse> accept(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(service.accept(id, auth));
    }

    @PostMapping("/{id}/reject") @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JoinRequestResponse> reject(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(service.reject(id, auth));
    }

    @DeleteMapping("/{id}") @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancel(@PathVariable UUID id, Authentication auth) {
        service.cancel(id, auth);
        return ResponseEntity.noContent().build();
    }
}
