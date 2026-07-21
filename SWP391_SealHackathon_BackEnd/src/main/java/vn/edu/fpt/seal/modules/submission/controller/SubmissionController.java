package vn.edu.fpt.seal.modules.submission.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.seal.modules.submission.dto.*;
import vn.edu.fpt.seal.modules.submission.service.SubmissionService;
import java.util.UUID;

@RestController
@RequestMapping("/submissions")
@RequiredArgsConstructor
@Tag(name = "Submissions", description = "Round project submissions")
public class SubmissionController {
    private final SubmissionService submissionService;
    @GetMapping @PreAuthorize("isAuthenticated()") @Operation(summary = "List submissions")
    public ResponseEntity<Page<SubmissionResponse>> list(@RequestParam UUID eventId, @RequestParam(required = false) UUID roundId, @RequestParam(required = false) UUID teamId, @RequestParam(required = false) UUID trackId, Pageable pageable, Authentication auth) { return ResponseEntity.ok(submissionService.list(eventId, roundId, teamId, trackId, pageable, auth)); }
    @GetMapping("/{id}") @PreAuthorize("isAuthenticated()") @Operation(summary = "Get submission")
    public ResponseEntity<SubmissionResponse> get(@PathVariable UUID id, Authentication auth) { return ResponseEntity.ok(submissionService.get(id, auth)); }
    @PostMapping @PreAuthorize("isAuthenticated()") @Operation(summary = "Create or update submission")
    public ResponseEntity<SubmissionResponse> submit(@Valid @RequestBody UpsertSubmissionRequest req, Authentication auth) { return ResponseEntity.ok(submissionService.submit(req, auth)); }
    @PatchMapping("/{id}") @PreAuthorize("isAuthenticated()") @Operation(summary = "Update submission")
    public ResponseEntity<SubmissionResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateSubmissionRequest req, Authentication auth) { return ResponseEntity.ok(submissionService.update(id, req, auth)); }
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('COORDINATOR')") @Operation(summary = "Delete submission")
    public ResponseEntity<Void> delete(@PathVariable UUID id) { submissionService.delete(id); return ResponseEntity.noContent().build(); }
}
