package vn.edu.fpt.seal.modules.ranking.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.seal.modules.ranking.dto.RecalculateRankingsRequest;
import vn.edu.fpt.seal.modules.ranking.dto.RoundRankingResponse;
import vn.edu.fpt.seal.modules.ranking.service.RoundRankingService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/round-rankings")
@RequiredArgsConstructor
@Tag(name = "Round Rankings")
public class RoundRankingController {
    private final RoundRankingService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<RoundRankingResponse>> list(@RequestParam UUID roundId, @RequestParam(required = false) UUID eventId,
                                                           @RequestParam(required = false) UUID trackId, Pageable pageable, Authentication authentication) {
        return ResponseEntity.ok(service.list(eventId, roundId, trackId, pageable, authentication));
    }

    @PostMapping("/rounds/{roundId}/recalculate")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<List<RoundRankingResponse>> recalculate(@PathVariable UUID roundId,
                                                                  @RequestParam(required = false, defaultValue = "false") boolean applyPromotion) {
        return ResponseEntity.ok(service.recalculate(roundId, new RecalculateRankingsRequest(applyPromotion)));
    }
}
