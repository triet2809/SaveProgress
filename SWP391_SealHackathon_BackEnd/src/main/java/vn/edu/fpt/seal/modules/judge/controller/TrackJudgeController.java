package vn.edu.fpt.seal.modules.judge.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.seal.modules.judge.dto.*;
import vn.edu.fpt.seal.modules.judge.service.TrackJudgeService;

import java.util.UUID;

@RestController @RequestMapping("/track-judges") @RequiredArgsConstructor @Tag(name = "Track Judges")
public class TrackJudgeController {
    private final TrackJudgeService service;
    @GetMapping @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<TrackJudgeResponse>> list(@RequestParam(required = false) UUID trackId, @RequestParam(required = false) UUID userId, Pageable pageable, Authentication authentication) { return ResponseEntity.ok(service.list(trackId, userId, pageable, authentication)); }
    @PostMapping @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<TrackJudgeResponse> assign(@Valid @RequestBody AssignTrackJudgeRequest req) { return ResponseEntity.ok(service.assign(req)); }
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<Void> remove(@PathVariable UUID id) { service.remove(id); return ResponseEntity.noContent().build(); }
    @DeleteMapping @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<Void> removeByTrackAndUser(@RequestParam UUID trackId, @RequestParam UUID userId) { service.removeByTrackAndUser(trackId, userId); return ResponseEntity.noContent().build(); }
}
