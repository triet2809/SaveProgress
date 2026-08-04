package vn.edu.fpt.seal.modules.chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.seal.modules.chat.dto.CreateTeamChatMessageRequest;
import vn.edu.fpt.seal.modules.chat.dto.TeamChatMessageResponse;
import vn.edu.fpt.seal.modules.chat.service.TeamChatService;
import vn.edu.fpt.seal.security.CurrentUser;

import java.util.List;
import java.util.UUID;

/**
 * REST controller cho chat nội bộ của đội.
 * Cho phép thành viên đội (và coordinator khi đọc) trao đổi tin nhắn.
 */
@RestController
@RequestMapping("/team-chat")
@RequiredArgsConstructor
public class TeamChatController {
    private final TeamChatService service;

    /**
     * Lấy danh sách tin nhắn của một đội; service kiểm tra quyền đọc.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TeamChatMessageResponse>> list(@RequestParam UUID teamId,
                                                              Authentication authentication) {
        return ResponseEntity.ok(service.list(teamId, authentication));
    }

    /**
     * Gửi tin nhắn mới; chỉ thành viên đội được gửi (kiểm tra ở service).
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamChatMessageResponse> create(@AuthenticationPrincipal CurrentUser user,
                                                          @Valid @RequestBody CreateTeamChatMessageRequest request) {
        return ResponseEntity.ok(service.create(request, user.getId()));
    }
}
