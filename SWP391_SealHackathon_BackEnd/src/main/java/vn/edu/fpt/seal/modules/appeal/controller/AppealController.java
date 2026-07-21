package vn.edu.fpt.seal.modules.appeal.controller;
import jakarta.validation.Valid; import lombok.RequiredArgsConstructor; import org.springframework.http.ResponseEntity; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.seal.modules.appeal.dto.AppealDtos; import vn.edu.fpt.seal.modules.appeal.service.AppealService; import java.util.*;
@RestController @RequestMapping("/appeals") @RequiredArgsConstructor public class AppealController{
 private final AppealService service;
 @PostMapping @PreAuthorize("isAuthenticated()") public ResponseEntity<AppealDtos.Response> create(@Valid @RequestBody AppealDtos.Create r,Authentication a){return ResponseEntity.ok(service.create(r,a));}
 @GetMapping @PreAuthorize("hasRole('COORDINATOR')") public ResponseEntity<List<AppealDtos.Response>> list(@RequestParam UUID eventId,@RequestParam(required=false)String status){return ResponseEntity.ok(service.list(eventId,status));}
 @GetMapping("/teams/{teamId}") @PreAuthorize("isAuthenticated()") public ResponseEntity<List<AppealDtos.Response>> team(@PathVariable UUID teamId,Authentication a){return ResponseEntity.ok(service.team(teamId,a));}
 @GetMapping("/{id}") @PreAuthorize("isAuthenticated()") public ResponseEntity<AppealDtos.Response> get(@PathVariable UUID id,Authentication a){return ResponseEntity.ok(service.get(id,a));}
 @PostMapping("/{id}/respond") @PreAuthorize("hasRole('COORDINATOR')") public ResponseEntity<AppealDtos.Response> respond(@PathVariable UUID id,@Valid @RequestBody AppealDtos.Respond r){return ResponseEntity.ok(service.respond(id,r));}
 @PostMapping("/{id}/resolve") @PreAuthorize("hasRole('COORDINATOR')") public ResponseEntity<AppealDtos.Response> resolve(@PathVariable UUID id,@Valid @RequestBody AppealDtos.Resolve r,Authentication a){return ResponseEntity.ok(service.resolve(id,r,a));}
}
