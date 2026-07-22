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

/**
 * Controller xử lý các endpoint xác thực: đăng ký, đăng nhập, làm mới token,
 * kích hoạt tài khoản, onboarding, đăng xuất và lấy thông tin người dùng hiện tại.
 * Ủy quyền nghiệp vụ cho {@link AuthService} và {@link AccountActivationService}.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication endpoints")
public class AuthController {

    private final AuthService authService;
    private final AccountActivationService activationService;

    /**
     * Kiểm tra token kích hoạt còn hợp lệ hay không (trước khi hiển form đặt mật khẩu).
     *
     * @param token token kích hoạt lấy từ query string
     * @return phản hồi cho biết token có hợp lệ hay không
     */
    @GetMapping("/activate/validate")
    public ResponseEntity<ActivationValidationResponse> validateActivation(@RequestParam String token) {
        activationService.validate(token);
        return ResponseEntity.ok(new ActivationValidationResponse(true, "Activation token is valid"));
    }

    /**
     * Kích hoạt tài khoản: đặt mật khẩu lần đầu bằng token hợp lệ.
     *
     * @param request dữ liệu kích hoạt (token, mật khẩu, xác nhận mật khẩu)
     * @return thông báo kích hoạt thành công
     */
    @PostMapping("/activate")
    public ResponseEntity<Map<String, Object>> activate(@Valid @RequestBody ActivationRequest request) {
        activationService.activate(request.token(), request.password(), request.confirmPassword());
        return ResponseEntity.ok(Map.of("success", true, "message", "Account activated"));
    }

    /**
     * Đăng ký tài khoản mới (trạng thái chờ duyệt).
     *
     * @param req dữ liệu đăng ký
     * @return phản hồi xác thực kèm token và thông tin người dùng
     */
    @PostMapping("/register")
    @Operation(summary = "Register new account (pending approval)")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    /**
     * Đăng nhập bằng email + mật khẩu.
     *
     * @param req thông tin đăng nhập
     * @return phản hồi xác thực kèm cặp token
     */
    @PostMapping("/login")
    @Operation(summary = "Login with email + password")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    /**
     * Làm mới access token bằng refresh token.
     *
     * @param req yêu cầu chứa refresh token
     * @return phản hồi xác thực với token mới
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return ResponseEntity.ok(authService.refresh(req));
    }

    /**
     * Hoàn tất onboarding bắt buộc: đổi mật khẩu và chấp nhận điều khoản pháp lý.
     * Chỉ cho người dùng đã đăng nhập; lấy ID người dùng từ principal.
     *
     * @param request        dữ liệu onboarding
     * @param authentication thông tin xác thực hiện tại
     * @return phản hồi xác thực đã cập nhật
     */
    @PostMapping("/onboarding")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Complete required password change and legal acceptance")
    public ResponseEntity<AuthResponse> onboarding(@Valid @RequestBody CompleteOnboardingRequest request,
                                                   Authentication authentication) {
        // Lấy CurrentUser từ principal để xác định chính xác người đang thao tác
        CurrentUser current = (CurrentUser) authentication.getPrincipal();
        return ResponseEntity.ok(authService.completeOnboarding(current.getId(), request));
    }

    /**
     * Đăng xuất phiên hiện tại: thu hồi access token (và refresh token nếu có).
     *
     * @param authorization header Authorization chứa access token
     * @param body          body tùy chọn chứa refreshToken cần thu hồi
     * @return thông báo đã đăng xuất
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Logout current session")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                       @RequestBody(required = false) Map<String, String> body) {
        authService.logout(authorization, body == null ? null : body.get("refreshToken"));
        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out. Access token revoked."));
    }

    /**
     * Lấy thông tin người dùng hiện tại từ token.
     *
     * @param user người dùng đã xác thực (inject từ principal)
     * @return id, email và danh sách vai trò của người dùng
     */
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
