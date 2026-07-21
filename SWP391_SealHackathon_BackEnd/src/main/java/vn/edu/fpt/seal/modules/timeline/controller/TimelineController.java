package vn.edu.fpt.seal.modules.timeline.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.seal.modules.timeline.dto.TimelineDtos.Response;
import vn.edu.fpt.seal.modules.timeline.service.TimelineService;
import vn.edu.fpt.seal.modules.timeline.TimelineEventType;
import vn.edu.fpt.seal.modules.timeline.TimelineScope;
import java.util.UUID;

@RestController @RequiredArgsConstructor
public class TimelineController {
    private final TimelineService service;
    @GetMapping("/events/{eventId}/timeline") @PreAuthorize("permitAll()")
    public ResponseEntity<Page<Response>> event(@PathVariable UUID eventId, @RequestParam(required=false) UUID roundId,
                                                @RequestParam(required=false) UUID trackId,
                                                @RequestParam(required=false) TimelineEventType eventType,
                                                @RequestParam(required=false) TimelineScope visibility,
                                                Pageable pageable, Authentication auth) {
        return ResponseEntity.ok(service.event(eventId, roundId, trackId, eventType, visibility, pageable, auth));
    }
    @GetMapping("/teams/{teamId}/timeline") @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Response>> team(@PathVariable UUID teamId, Pageable pageable, Authentication auth) {
        return ResponseEntity.ok(service.team(teamId, pageable, auth));
    }
    @GetMapping("/timeline/{id}") @PreAuthorize("permitAll()")
    public ResponseEntity<Response> get(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(service.get(id, auth));
    }
}
