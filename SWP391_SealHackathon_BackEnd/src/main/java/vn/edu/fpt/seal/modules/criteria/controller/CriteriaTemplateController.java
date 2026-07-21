package vn.edu.fpt.seal.modules.criteria.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.seal.modules.criteria.dto.*;
import vn.edu.fpt.seal.modules.criteria.service.CriteriaTemplateService;

import java.util.UUID;

@RestController @RequestMapping("/criteria-templates") @RequiredArgsConstructor @Tag(name = "Criteria Templates")
public class CriteriaTemplateController {
    private final CriteriaTemplateService service;
    @GetMapping @PreAuthorize("isAuthenticated()") public ResponseEntity<Page<CriteriaTemplateResponse>> list(Pageable pageable) { return ResponseEntity.ok(service.list(pageable)); }
    @GetMapping("/{id}") @PreAuthorize("isAuthenticated()") public ResponseEntity<CriteriaTemplateResponse> get(@PathVariable UUID id) { return ResponseEntity.ok(service.get(id)); }
    @PostMapping @PreAuthorize("hasRole('COORDINATOR')") public ResponseEntity<CriteriaTemplateResponse> create(@Valid @RequestBody CreateCriteriaTemplateRequest req) { return ResponseEntity.ok(service.create(req)); }
    @PatchMapping("/{id}") @PreAuthorize("hasRole('COORDINATOR')") public ResponseEntity<CriteriaTemplateResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateCriteriaTemplateRequest req) { return ResponseEntity.ok(service.update(id, req)); }
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('COORDINATOR')") public ResponseEntity<Void> delete(@PathVariable UUID id) { service.delete(id); return ResponseEntity.noContent().build(); }
}
