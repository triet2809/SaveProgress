package vn.edu.fpt.seal.modules.recognition.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.seal.modules.recognition.dto.RecognitionDtos;
import vn.edu.fpt.seal.modules.recognition.service.TeamRecognitionService;

import java.util.UUID;

@RestController
@RequestMapping("/team-profiles/{profileId}/recognitions")
@RequiredArgsConstructor
public class TeamRecognitionController {
    private final TeamRecognitionService service;

    @PostMapping("/recalculate")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<RecognitionDtos.EvidenceResponse> recalculate(
            @PathVariable UUID profileId, Authentication authentication) {
        return ResponseEntity.ok(service.recalculate(profileId, authentication));
    }

    @GetMapping("/evidence")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<RecognitionDtos.EvidenceResponse> evidence(@PathVariable UUID profileId) {
        return ResponseEntity.ok(service.evidence(profileId));
    }

    @PostMapping("/{recognitionId}/revoke")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<RecognitionDtos.EvidenceResponse> revoke(
            @PathVariable UUID profileId,
            @PathVariable UUID recognitionId,
            @Valid @RequestBody RecognitionDtos.RevokeRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(service.revoke(profileId, recognitionId, request, authentication));
    }

    @PostMapping("/{recognitionId}/restore")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<RecognitionDtos.EvidenceResponse> restore(
            @PathVariable UUID profileId,
            @PathVariable UUID recognitionId,
            Authentication authentication) {
        return ResponseEntity.ok(service.restore(profileId, recognitionId, authentication));
    }
}
