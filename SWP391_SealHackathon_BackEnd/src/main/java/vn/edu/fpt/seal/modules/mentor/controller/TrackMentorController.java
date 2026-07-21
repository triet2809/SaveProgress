package vn.edu.fpt.seal.modules.mentor.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.seal.modules.mentor.dto.*;
import vn.edu.fpt.seal.modules.mentor.service.TrackMentorService;

import java.util.*;

@RestController @RequestMapping("/track-mentors") @RequiredArgsConstructor @Tag(name = "Track Mentors")
public class TrackMentorController {
    private final TrackMentorService service;
    @GetMapping @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<TrackMentorResponse>> list(@RequestParam(required = false) UUID eventId, @RequestParam(required = false) UUID trackId, @RequestParam(required = false) UUID userId, Pageable pageable, Authentication authentication) { return ResponseEntity.ok(service.list(eventId, trackId, userId, pageable, authentication)); }
    @PostMapping @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<TrackMentorResponse> assign(@Valid @RequestBody AssignTrackMentorRequest req) { return ResponseEntity.ok(service.assign(req)); }
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<Void> remove(@PathVariable UUID id) { service.remove(id); return ResponseEntity.noContent().build(); }
    @DeleteMapping @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<Void> removeByTrackAndUser(@RequestParam UUID trackId, @RequestParam UUID userId) { service.removeByTrackAndUser(trackId, userId); return ResponseEntity.noContent().build(); }
    @GetMapping("/mentors/{mentorId}/teams") @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MentorTeamResponse>> teams(@PathVariable UUID mentorId, @RequestParam(required=false) UUID eventId, @RequestParam(required=false) UUID trackId, @RequestParam(required=false) UUID roundId, Authentication authentication) { return ResponseEntity.ok(service.teams(mentorId, eventId, trackId, roundId, authentication)); }
    @GetMapping("/me/teams") @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<List<MentorTeamResponse>> myTeams(@RequestParam(required=false) UUID eventId, @RequestParam(required=false) UUID trackId, @RequestParam(required=false) UUID roundId, Authentication authentication) {
        return ResponseEntity.ok(service.teams(service.currentMentorId(authentication), eventId, trackId, roundId, authentication));
    }
}
