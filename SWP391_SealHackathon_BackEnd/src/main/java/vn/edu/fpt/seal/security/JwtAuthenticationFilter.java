package vn.edu.fpt.seal.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Filter chạy một lần cho mỗi request, chịu trách nhiệm xác thực dựa trên JWT.
 *
 * <p>Luồng xử lý: trích access token từ header Authorization, xác minh chữ ký,
 * kiểm tra token có bị thu hồi, kiểm tra trạng thái tài khoản và securityVersion,
 * sau đó nạp {@link CurrentUser} vào SecurityContext. Nếu token yêu cầu onboarding
 * mà request không thuộc các đường dẫn onboarding cho phép, trả về 449.</p>
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Dịch vụ xử lý JWT (phân tích, xác minh, kiểm tra revoke).
     */
    private final JwtService jwtService;
    /**
     * Repository người dùng để kiểm tra trạng thái/securityVersion (có thể null trong test).
     */
    private final UserRepository userRepository;

    /**
     * Constructor chính dùng cho Spring inject.
     *
     * @param jwtService     dịch vụ JWT
     * @param userRepository repository người dùng (có thể null)
     */
    @Autowired
    public JwtAuthenticationFilter(JwtService jwtService, @Nullable UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    /**
     * Constructor rút gọn (không kiểm tra DB), hữu ích cho unit test.
     *
     * @param jwtService dịch vụ JWT
     */
    public JwtAuthenticationFilter(JwtService jwtService) {
        this(jwtService, null);
    }

    /**
     * Xử lý chính cho mỗi request: xác thực JWT và thiết lập SecurityContext.
     *
     * @param request  request đến
     * @param response response trả về
     * @param chain    chuỗi filter tiếp theo
     * @throws ServletException nếu lỗi servlet
     * @throws IOException      nếu lỗi I/O
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String token = extractToken(request);
        // Chỉ xử lý khi có token và chưa có authentication trong context
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtService.parseToken(token);
                // Từ chối token đã bị thu hồi
                if (jwtService.isRevoked(token)) {
                    throw new JwtException("JWT has been revoked");
                }
                if (jwtService.isAccessToken(claims)) {
                    UUID userId = UUID.fromString(claims.getSubject());
                    if (userRepository != null) {
                        // Đối chiếu securityVersion trong token với DB để vô hiệu token cũ
                        // sau khi người dùng đổi mật khẩu/thu hồi quyền
                        Long tokenVersion = jwtService.securityVersion(claims);
                        if (tokenVersion == null) throw new JwtException("Missing or malformed security version");
                        var user = userRepository.findById(userId)
                                .orElseThrow(() -> new JwtException("User not found"));
                        // Chỉ chấp nhận tài khoản đã duyệt và securityVersion khớp
                        if (user.getStatus() != vn.edu.fpt.seal.common.enums.AccountStatus.approved
                                || user.getSecurityVersion() != tokenVersion) {
                            throw new JwtException("Account status or security version is invalid");
                        }
                    }
                    String email = claims.get("email", String.class);
                    @SuppressWarnings("unchecked")
                    List<String> roles = (List<String>) claims.getOrDefault("roles", List.of());

                    // Chuyển role thành authority theo quy ước ROLE_<TÊN_VIẾT_HOA>
                    var authorities = roles.stream()
                            .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()))
                            .toList();

                    CurrentUser principal = CurrentUser.builder()
                            .id(userId)
                            .email(email)
                            .roles(roles)
                            .build();

                    var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    if (Boolean.TRUE.equals(claims.get("onboarding_required", Boolean.class))
                            && !isOnboardingPath(request.getRequestURI(), request.getContextPath())) {
                        // Trả 449 Upgrade Required để frontend phân biệt trường hợp cần onboarding
                        // với lỗi 403 Forbidden thực sự do RBAC từ chối.
                        response.setStatus(449);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"message\":\"Complete password and terms onboarding before continuing\",\"code\":\"ONBOARDING_REQUIRED\"}");
                        return;
                    }
                }
            } catch (JwtException | IllegalArgumentException e) {
                // Token không hợp lệ: xoá context, request sẽ bị coi là chưa xác thực
                log.debug("Invalid JWT: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * Trích access token từ header Authorization dạng "Bearer &lt;token&gt;".
     *
     * @param request request đến
     * @return chuỗi token, hoặc null nếu không có
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    /**
     * Kiểm tra đường dẫn có thuộc nhóm endpoint được phép khi đang trong trạng thái onboarding.
     * Loại bỏ tiền tố context path trước khi so khớp.
     *
     * @param path        URI của request
     * @param contextPath context path của ứng dụng
     * @return true nếu là đường dẫn onboarding/logout/me/refresh
     */
    private boolean isOnboardingPath(String path, String contextPath) {
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return path.equals("/auth/onboarding") || path.equals("/auth/logout")
                || path.equals("/auth/me") || path.equals("/auth/refresh");
    }
}
