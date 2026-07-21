package vn.edu.fpt.seal.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.config.AppProperties;
import vn.edu.fpt.seal.modules.auth.entity.RevokedToken;
import vn.edu.fpt.seal.modules.auth.repository.RevokedTokenRepository;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final AppProperties appProperties;
    private final RevokedTokenRepository revokedTokenRepository;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(
                appProperties.getSecurity().getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, String email, List<String> roles) {
        return generateAccessToken(userId, email, roles, false, 1L);
    }

    public String generateAccessToken(UUID userId, String email, List<String> roles, boolean onboardingRequired) {
        return generateAccessToken(userId, email, roles, onboardingRequired, 1L);
    }

    public String generateAccessToken(UUID userId, String email, List<String> roles,
                                      boolean onboardingRequired, long securityVersion) {
        var jwt = appProperties.getSecurity().getJwt();
        Instant now = Instant.now();
        Instant exp = now.plus(jwt.getAccessTokenExpirationMinutes(), ChronoUnit.MINUTES);

        return Jwts.builder()
                .issuer(jwt.getIssuer())
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles)
                .claim("onboarding_required", onboardingRequired)
                .claim("security_version", securityVersion)
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey())
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        return generateRefreshToken(userId, 1L);
    }

    public String generateRefreshToken(UUID userId, long securityVersion) {
        var jwt = appProperties.getSecurity().getJwt();
        Instant now = Instant.now();
        Instant exp = now.plus(jwt.getRefreshTokenExpirationDays(), ChronoUnit.DAYS);

        return Jwts.builder()
                .issuer(jwt.getIssuer())
                .subject(userId.toString())
                .claim("security_version", securityVersion)
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isAccessToken(Claims claims) {
        return "access".equals(claims.get("type", String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return "refresh".equals(claims.get("type", String.class));
    }

    public Long securityVersion(Claims claims) {
        Object value = claims.get("security_version");
        if (!(value instanceof Number number)) return null;
        long version = number.longValue();
        return version > 0 && number.doubleValue() == version ? version : null;
    }

    @Transactional
    public void revokeToken(String token) {
        Claims claims = parseToken(token);
        Date expiration = claims.getExpiration();
        if (expiration == null || expiration.toInstant().isBefore(Instant.now())) {
            return;
        }
        String hash = hashToken(token);
        if (!revokedTokenRepository.existsByTokenHashAndExpiresAtAfter(hash, Instant.now())) {
            revokedTokenRepository.save(RevokedToken.builder()
                    .tokenHash(hash)
                    .expiresAt(expiration.toInstant())
                    .build());
        }
        revokedTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }

    public boolean isRevoked(String token) {
        return revokedTokenRepository.existsByTokenHashAndExpiresAtAfter(hashToken(token), Instant.now());
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot hash token", e);
        }
    }
}
