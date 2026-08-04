package vn.edu.fpt.seal.modules.notice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.seal.modules.notice.dto.CreateNoticeRequest;
import vn.edu.fpt.seal.modules.notice.dto.NoticeResponse;
import vn.edu.fpt.seal.modules.notice.service.NoticeService;
import vn.edu.fpt.seal.security.CurrentUser;

import java.util.UUID;

@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
public class NoticeController {
    private final NoticeService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<NoticeResponse>> list(@RequestParam(required = false) String targetRole, @RequestParam(required = false) UUID eventId, @RequestParam(required = false) UUID trackId, Pageable p) {
        return ResponseEntity.ok(service.list(targetRole, eventId, trackId, p));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COORDINATOR','MENTOR','JUDGE')")
    public ResponseEntity<NoticeResponse> create(@AuthenticationPrincipal CurrentUser user, @Valid @RequestBody CreateNoticeRequest r) {
        return ResponseEntity.ok(service.create(r, user.getId()));
    }
}
