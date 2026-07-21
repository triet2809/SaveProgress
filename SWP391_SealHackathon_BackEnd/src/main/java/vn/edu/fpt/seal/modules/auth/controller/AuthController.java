package vn.edu.fpt.seal.modules.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.seal.modules.auth.dto.AuthResponse;
import vn.edu.fpt.seal.modules.auth.dto.LoginRequest;
import vn.edu.fpt.seal.modules.auth.dto.RefreshRequest;
import vn.edu.fpt.seal.modules.auth.dto.RegisterRequest;
import vn.edu.fpt.seal.modules.auth.service.AuthService;
import vn.edu.fpt.seal.modules.auth.service.AccountActivationService;
import vn.edu.fpt.seal.modules.auth.dto.ActivationRequest;
import vn.edu.fpt.seal.modules.auth.dto.ActivationValidationResponse;
import vn.edu.fpt.seal.security.CurrentUser;

import java.util.Map;
import org.springframework.security.core.Authentication;
import vn.edu.fpt.seal.modules.auth.dto.CompleteOnboardingRequest;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication endpoints")
public class AuthController {

    private final AuthService authService;
    private final AccountActivationService activationService;

    @GetMapping("/activate/validate")
    public ResponseEntity<ActivationValidationResponse> validateActivation(@RequestParam String token) {
        activationService.validate(token);
        return ResponseEntity.ok(new ActivationValidationResponse(true, "Activation token is valid"));
    }

    @PostMapping("/activate")
    public ResponseEntity<Map<String, Object>> activate(@Valid @RequestBody ActivationRequest request) {
        activationService.activate(request.token(), request.password(), request.confirmPassword());
        return ResponseEntity.ok(Map.of("success", true, "message", "Account activated"));
    }

    @PostMapping("/register")
    @Operation(summary = "Register new account (pending approval)")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email + password")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return ResponseEntity.ok(authService.refresh(req));
    }

    @PostMapping("/onboarding")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Complete required password change and legal acceptance")
    public ResponseEntity<AuthResponse> onboarding(@Valid @RequestBody CompleteOnboardingRequest request,
                                                   Authentication authentication) {
        CurrentUser current = (CurrentUser) authentication.getPrincipal();
        return ResponseEntity.ok(authService.completeOnboarding(current.getId(), request));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Logout current session")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                       @RequestBody(required = false) Map<String, String> body) {
        authService.logout(authorization, body == null ? null : body.get("refreshToken"));
        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out. Access token revoked."));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user info from token")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal CurrentUser user) {
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "roles", user.getRoles()
        ));
    }
}
