package vn.edu.fpt.seal.modules.team.controller;

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
import vn.edu.fpt.seal.modules.team.dto.*;
import vn.edu.fpt.seal.modules.team.service.TeamService;
import vn.edu.fpt.seal.modules.team.service.TeamTransferService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
@Tag(name = "Teams", description = "Hackathon teams and team members")
public class TeamController {
    private final TeamService teamService;
    private final TeamTransferService transferService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List teams by track")
    public ResponseEntity<Page<TeamResponse>> listByTrack(@RequestParam UUID eventId, @RequestParam(required = false) UUID trackId, Pageable pageable) {
        return ResponseEntity.ok(teamService.listByTrack(eventId, trackId, pageable));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List teams for current user")
    public ResponseEntity<List<TeamResponse>> myTeams(Authentication auth) {
        return ResponseEntity.ok(teamService.myTeams(auth));
    }

    @PostMapping("/join")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Join team by invite code")
    public ResponseEntity<TeamResponse> join(@Valid @RequestBody JoinTeamRequest req, Authentication auth) {
        return ResponseEntity.ok(teamService.joinByInviteCode(req, auth));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get team by id")
    public ResponseEntity<TeamResponse> get(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(teamService.get(id, auth));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create team (coordinator, or a team leader self-creating)")
    public ResponseEntity<TeamResponse> create(@Valid @RequestBody CreateTeamRequest req, Authentication auth) {
        return ResponseEntity.ok(teamService.create(req, auth));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Update team")
    public ResponseEntity<TeamResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateTeamRequest req) {
        return ResponseEntity.ok(teamService.update(id, req));
    }

    @PostMapping("/{id}/move-track")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Move team to another track in the same event")
    public ResponseEntity<TeamResponse> moveTrack(@PathVariable UUID id, @Valid @RequestBody MoveTeamTrackRequest req, Authentication auth) {
        return ResponseEntity.ok(teamService.moveToTrack(id, req, auth));
    }

    @PostMapping("/bulk-transfer")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Move selected teams atomically")
    public ResponseEntity<TeamTransferDtos.TransferResult> bulkTransfer(@Valid @RequestBody TeamTransferDtos.BulkTransferRequest req) {
        return ResponseEntity.ok(transferService.bulkTransfer(req));
    }

    @PostMapping("/events/{eventId}/balance-preview")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Preview deterministic balanced team distribution")
    public ResponseEntity<TeamTransferDtos.TransferResult> balancePreview(@PathVariable UUID eventId, @Valid @RequestBody TeamTransferDtos.BalanceRequest req) {
        return ResponseEntity.ok(transferService.previewBalance(eventId, req));
    }

    @PostMapping("/events/{eventId}/balance-apply")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Apply deterministic balanced team distribution")
    public ResponseEntity<TeamTransferDtos.TransferResult> balanceApply(@PathVariable UUID eventId, @Valid @RequestBody TeamTransferDtos.BalanceRequest req) {
        return ResponseEntity.ok(transferService.applyBalance(eventId, req));
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Add member")
    public ResponseEntity<TeamResponse> addMember(@PathVariable UUID id, @Valid @RequestBody AddTeamMemberRequest req) {
        return ResponseEntity.ok(teamService.addMember(id, req));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Remove member")
    public ResponseEntity<Void> removeMember(@PathVariable UUID id, @PathVariable UUID userId) {
        teamService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/disqualify")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Disqualify team")
    public ResponseEntity<TeamResponse> disqualify(@PathVariable UUID id, @Valid @RequestBody DisqualifyTeamRequest req, Authentication auth) {
        return ResponseEntity.ok(teamService.disqualify(id, req, auth));
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Reactivate team")
    public ResponseEntity<TeamResponse> reactivate(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(teamService.reactivate(id, auth));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Delete team")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        teamService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
