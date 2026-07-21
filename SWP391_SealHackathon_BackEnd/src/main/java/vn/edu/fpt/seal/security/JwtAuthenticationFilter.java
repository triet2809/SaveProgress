package vn.edu.fpt.seal.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Autowired
    public JwtAuthenticationFilter(JwtService jwtService, @Nullable UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public JwtAuthenticationFilter(JwtService jwtService) {
        this(jwtService, null);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtService.parseToken(token);
                if (jwtService.isRevoked(token)) {
                    throw new JwtException("JWT has been revoked");
                }
                if (jwtService.isAccessToken(claims)) {
                    UUID userId = UUID.fromString(claims.getSubject());
                    if (userRepository != null) {
                        Long tokenVersion = jwtService.securityVersion(claims);
                        if (tokenVersion == null) throw new JwtException("Missing or malformed security version");
                        var user = userRepository.findById(userId)
                                .orElseThrow(() -> new JwtException("User not found"));
                        if (user.getStatus() != vn.edu.fpt.seal.common.enums.AccountStatus.approved
                                || user.getSecurityVersion() != tokenVersion) {
                            throw new JwtException("Account status or security version is invalid");
                        }
                    }
                    String email = claims.get("email", String.class);
                    @SuppressWarnings("unchecked")
                    List<String> roles = (List<String>) claims.getOrDefault("roles", List.of());

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
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"message\":\"Complete password and terms onboarding before continuing\"}");
                        return;
                    }
                }
            } catch (JwtException | IllegalArgumentException e) {
                log.debug("Invalid JWT: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private boolean isOnboardingPath(String path, String contextPath) {
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return path.equals("/auth/onboarding") || path.equals("/auth/logout")
                || path.equals("/auth/me") || path.equals("/auth/refresh");
    }
}
