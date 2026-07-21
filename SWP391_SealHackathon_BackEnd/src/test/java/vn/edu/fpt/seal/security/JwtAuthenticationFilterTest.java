package vn.edu.fpt.seal.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;
import vn.edu.fpt.seal.common.enums.AccountStatus;
import vn.edu.fpt.seal.modules.user.entity.User;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtAuthenticationFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void contextPathDoesNotBlockOnboardingEndpoint() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        Claims claims = Jwts.claims()
                .subject(UUID.randomUUID().toString())
                .add("email", "judge@example.test")
                .add("roles", List.of("judge"))
                .add("onboarding_required", true)
                .build();
        when(jwtService.parseToken(anyString())).thenReturn(claims);
        when(jwtService.isRevoked(anyString())).thenReturn(false);
        when(jwtService.isAccessToken(claims)).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/onboarding");
        request.setContextPath("/api");
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        org.junit.jupiter.api.Assertions.assertNotEquals(403, response.getStatus());
    }

    @Test
    void missingSecurityVersionClaimIsRejected() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        UserRepository users = mock(UserRepository.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, users);
        Claims claims = Jwts.claims().subject(UUID.randomUUID().toString()).build();
        when(jwtService.parseToken(anyString())).thenReturn(claims);
        when(jwtService.isRevoked(anyString())).thenReturn(false);
        when(jwtService.isAccessToken(claims)).thenReturn(true);
        when(jwtService.securityVersion(claims)).thenReturn(null);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader("Authorization", "Bearer token");
        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(any(), any());
        verifyNoInteractions(users);
    }

    @Test
    void staleSecurityVersionClaimIsRejected() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        UserRepository users = mock(UserRepository.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, users);
        UUID id = UUID.randomUUID();
        Claims claims = Jwts.claims().subject(id.toString()).add("email", "approved@example.test")
                .add("roles", List.of("judge")).build();
        User user = User.builder().email("approved@example.test").fullName("Approved")
                .status(AccountStatus.approved).securityVersion(2L).build();
        user.setId(id);
        when(jwtService.parseToken(anyString())).thenReturn(claims);
        when(jwtService.isRevoked(anyString())).thenReturn(false);
        when(jwtService.isAccessToken(claims)).thenReturn(true);
        when(jwtService.securityVersion(claims)).thenReturn(1L);
        when(users.findById(id)).thenReturn(java.util.Optional.of(user));
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader("Authorization", "Bearer token");
        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(any(), any());
    }
}
