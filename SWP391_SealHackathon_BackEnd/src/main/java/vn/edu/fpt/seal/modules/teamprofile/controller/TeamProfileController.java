package vn.edu.fpt.seal.modules.teamprofile.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.seal.modules.team.dto.TeamResponse;
import vn.edu.fpt.seal.modules.teamprofile.dto.TeamProfileDtos;
import vn.edu.fpt.seal.modules.teamprofile.service.TeamProfileService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/team-profiles")
@RequiredArgsConstructor
public class TeamProfileController {
    private final TeamProfileService service;

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TeamProfileDtos.ProfileSummary>> mine(
            @RequestParam(required = false) UUID targetEventId,
            Authentication authentication) {
        return ResponseEntity.ok(service.mine(targetEventId, authentication));
    }

    @PostMapping("/{profileId}/reactivation-preview")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamProfileDtos.PreviewResponse> preview(
            @PathVariable UUID profileId,
            @Valid @RequestBody TeamProfileDtos.ReactivationRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(service.preview(profileId, request, authentication));
    }

    @PostMapping("/{profileId}/reactivate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamResponse> reactivate(
            @PathVariable UUID profileId,
            @Valid @RequestBody TeamProfileDtos.ReactivationRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(service.reactivate(profileId, request, authentication));
    }
}
