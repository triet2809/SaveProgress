package vn.edu.fpt.seal.modules.track.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.seal.modules.track.dto.CreateTrackRequest;
import vn.edu.fpt.seal.modules.track.dto.TrackResponse;
import vn.edu.fpt.seal.modules.track.dto.UpdateTrackRequest;
import vn.edu.fpt.seal.modules.track.service.TrackService;

import java.util.UUID;

@RestController
@RequestMapping("/tracks")
@RequiredArgsConstructor
@Tag(name = "Tracks", description = "Competition tracks (categories) within an event")
public class TrackController {

    private final TrackService trackService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List tracks by event")
    public ResponseEntity<Page<TrackResponse>> listByEvent(
            @RequestParam(required = false) UUID eventId, Pageable pageable) {
        return ResponseEntity.ok(trackService.listByEvent(eventId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get track by id")
    public ResponseEntity<TrackResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(trackService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Create track (coordinator only)")
    public ResponseEntity<TrackResponse> create(@Valid @RequestBody CreateTrackRequest req) {
        return ResponseEntity.ok(trackService.create(req));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Update track (coordinator only)")
    public ResponseEntity<TrackResponse> update(@PathVariable UUID id,
                                                @Valid @RequestBody UpdateTrackRequest req) {
        return ResponseEntity.ok(trackService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Delete track (event must still be draft)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        trackService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
