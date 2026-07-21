package vn.edu.fpt.seal.modules.support.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.seal.modules.support.dto.CreateSupportTicketRequest;
import vn.edu.fpt.seal.modules.support.dto.SupportTicketResponse;
import vn.edu.fpt.seal.modules.support.dto.UpdateSupportTicketStatusRequest;
import vn.edu.fpt.seal.modules.support.service.SupportTicketService;
import vn.edu.fpt.seal.security.CurrentUser;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/support-tickets")
@RequiredArgsConstructor
public class SupportTicketController {
    private final SupportTicketService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SupportTicketResponse>> list(@RequestParam(required = false) UUID requesterId,
                                                            Authentication authentication) {
        return ResponseEntity.ok(service.list(requesterId, authentication));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SupportTicketResponse> get(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(service.get(id, authentication));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SupportTicketResponse> create(@AuthenticationPrincipal CurrentUser user,
                                                        @Valid @RequestBody CreateSupportTicketRequest request) {
        return ResponseEntity.ok(service.create(request, user.getId()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<SupportTicketResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSupportTicketStatusRequest request) {
        return ResponseEntity.ok(service.updateStatus(id, request));
    }
}
