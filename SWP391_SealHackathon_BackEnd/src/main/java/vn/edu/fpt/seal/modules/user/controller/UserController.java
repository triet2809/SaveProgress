package vn.edu.fpt.seal.modules.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.seal.common.enums.AccountStatus;
import vn.edu.fpt.seal.modules.user.dto.*;
import vn.edu.fpt.seal.modules.user.service.UserImportService;
import vn.edu.fpt.seal.modules.user.service.UserService;
import vn.edu.fpt.seal.security.CurrentUser;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService service;
    private final UserImportService importService;

    @GetMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<Page<UserResponse>> list(@RequestParam(required = false) AccountStatus status, @RequestParam(required = false) String email, @RequestParam(required = false) String role, Pageable p) {
        return ResponseEntity.ok(service.list(status, email, role, p));
    }

    @PostMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest r) {
        return ResponseEntity.ok(service.create(r));
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<ImportUsersResponse> importUsers(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(importService.importExternalStudents(file));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal CurrentUser user) {
        return ResponseEntity.ok(service.get(user.getId()));
    }

    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> updateMe(@AuthenticationPrincipal CurrentUser user, @Valid @RequestBody UpdateCurrentUserRequest r) {
        return ResponseEntity.ok(service.updateCurrentUser(user.getId(), r));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<UserResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<UserResponse> updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateUserStatusRequest r,
                                                      @AuthenticationPrincipal CurrentUser actor) {
        return ResponseEntity.ok(service.updateStatus(id, r, actor == null ? null : actor.getId()));
    }

    @PatchMapping("/{id}/profile")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<UserResponse> updateProfile(@PathVariable UUID id, @Valid @RequestBody UpdateCurrentUserRequest r) {
        return ResponseEntity.ok(service.updateProfile(id, r));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<UserResponse> updateRoles(@PathVariable UUID id, @Valid @RequestBody UpdateUserRolesRequest r) {
        return ResponseEntity.ok(service.updateRoles(id, r));
    }
}
