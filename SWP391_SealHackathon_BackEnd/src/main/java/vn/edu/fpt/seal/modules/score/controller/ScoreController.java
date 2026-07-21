package vn.edu.fpt.seal.modules.score.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.seal.modules.score.dto.*;
import vn.edu.fpt.seal.modules.score.service.ScoreService;
import java.util.UUID;

@RestController @RequestMapping("/scores") @RequiredArgsConstructor @Tag(name="Scores")
public class ScoreController {
    private final ScoreService service;
    @GetMapping @PreAuthorize("isAuthenticated()") public ResponseEntity<Page<ScoreResponse>> list(@RequestParam(required=false) UUID submissionId,@RequestParam(required=false) UUID judgeId, Pageable pageable, Authentication auth){return ResponseEntity.ok(service.list(submissionId,judgeId,pageable,auth));}
    @GetMapping("/{id}") @PreAuthorize("isAuthenticated()") public ResponseEntity<ScoreResponse> get(@PathVariable UUID id, Authentication auth){return ResponseEntity.ok(service.get(id,auth));}
    @PostMapping @PreAuthorize("hasRole('JUDGE')") public ResponseEntity<ScoreResponse> upsert(@Valid @RequestBody UpsertScoreRequest req, Authentication auth){return ResponseEntity.ok(service.upsert(req,auth));}
    @DeleteMapping("/{id}") @PreAuthorize("hasAnyRole('JUDGE','COORDINATOR')") public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication auth){service.delete(id,auth); return ResponseEntity.noContent().build();}
}
