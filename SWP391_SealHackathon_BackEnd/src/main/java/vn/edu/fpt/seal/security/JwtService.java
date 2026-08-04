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
import java.util.UUID;

/**
 * Dịch vụ sinh, phân tích, xác minh và thu hồi (revoke) JSON Web Token.
 *
 * <p>Hỗ trợ hai loại token: access (ngắn hạn, mang thông tin người dùng/role)
 * và refresh (dài hạn, dùng để cấp lại access token). Token được ký bằng HMAC với
 * khóa bí mật trong cấu hình. Cơ chế revoke lưu hash SHA-256 của token vào DB
 * để vô hiệu hóa token trước khi hết hạn.</p>
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    /**
     * Cấu hình ứng dụng chứa khóa bí mật, issuer và thời hạn token.
     */
    private final AppProperties appProperties;
    /**
     * Repository lưu trữ hash của các token đã bị thu hồi.
     */
    private final RevokedTokenRepository revokedTokenRepository;

    /**
     * Tạo khóa ký HMAC từ secret trong cấu hình.
     */
    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(
                appProperties.getSecurity().getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Sinh access token với giá trị mặc định (không yêu cầu onboarding, securityVersion = 1).
     *
     * @param userId id người dùng
     * @param email  email người dùng
     * @param roles  danh sách role
     * @return chuỗi access token đã ký
     */
    public String generateAccessToken(UUID userId, String email, List<String> roles) {
        return generateAccessToken(userId, email, roles, false, 1L);
    }

    /**
     * Sinh access token có cờ onboarding, securityVersion mặc định = 1.
     *
     * @param onboardingRequired có yêu cầu hoàn tất onboarding hay không
     * @return chuỗi access token đã ký
     */
    public String generateAccessToken(UUID userId, String email, List<String> roles, boolean onboardingRequired) {
        return generateAccessToken(userId, email, roles, onboardingRequired, 1L);
    }

    /**
     * Sinh access token đầy đủ tham số.
     *
     * @param userId             id người dùng (subject)
     * @param email              email nhúng vào claim
     * @param roles              danh sách role nhúng vào claim
     * @param onboardingRequired cờ báo cần hoàn tất onboarding
     * @param securityVersion    phiên bản bảo mật để vô hiệu toàn bộ token cũ khi thay đổi
     * @return chuỗi access token đã ký, kèm thời điểm phát hành và hết hạn
     */
    public String generateAccessToken(UUID userId, String email, List<String> roles,
                                      boolean onboardingRequired, long securityVersion) {
        var jwt = appProperties.getSecurity().getJwt();
        Instant now = Instant.now();
        // Tính thời điểm hết hạn theo số phút cấu hình
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

    /**
     * Sinh refresh token với securityVersion mặc định = 1.
     *
     * @param userId id người dùng
     * @return chuỗi refresh token đã ký
     */
    public String generateRefreshToken(UUID userId) {
        return generateRefreshToken(userId, 1L);
    }

    /**
     * Sinh refresh token (dài hạn) dùng để cấp lại access token.
     *
     * @param userId          id người dùng (subject)
     * @param securityVersion phiên bản bảo mật
     * @return chuỗi refresh token đã ký
     */
    public String generateRefreshToken(UUID userId, long securityVersion) {
        var jwt = appProperties.getSecurity().getJwt();
        Instant now = Instant.now();
        // Refresh token hết hạn theo số ngày cấu hình
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

    /**
     * Phân tích và xác minh chữ ký token, trả về các claim.
     *
     * @param token chuỗi JWT
     * @return {@link Claims} đã xác minh
     * @throws io.jsonwebtoken.JwtException nếu chữ ký sai hoặc token hết hạn
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Kiểm tra token có phải loại access hay không.
     *
     * @param claims claim của token
     * @return true nếu claim {@code type} = "access"
     */
    public boolean isAccessToken(Claims claims) {
        return "access".equals(claims.get("type", String.class));
    }

    /**
     * Kiểm tra token có phải loại refresh hay không.
     *
     * @param claims claim của token
     * @return true nếu claim {@code type} = "refresh"
     */
    public boolean isRefreshToken(Claims claims) {
        return "refresh".equals(claims.get("type", String.class));
    }

    /**
     * Lấy securityVersion từ claim một cách an toàn.
     *
     * @param claims claim của token
     * @return giá trị securityVersion nếu là số nguyên dương hợp lệ, ngược lại null
     */
    public Long securityVersion(Claims claims) {
        Object value = claims.get("security_version");
        if (!(value instanceof Number number)) return null;
        long version = number.longValue();
        // Chỉ chấp nhận số nguyên dương, không chấp nhận phần thập phân
        return version > 0 && number.doubleValue() == version ? version : null;
    }

    /**
     * Thu hồi token: lưu hash vào DB để từ chối các lần sử dụng sau.
     * Bỏ qua nếu token đã hết hạn; đồng thời dọn các bản ghi revoke đã hết hạn.
     *
     * @param token chuỗi JWT cần thu hồi
     */
    @Transactional
    public void revokeToken(String token) {
        Claims claims = parseToken(token);
        Date expiration = claims.getExpiration();
        // Token đã hết hạn thì không cần lưu vào danh sách revoke
        if (expiration == null || expiration.toInstant().isBefore(Instant.now())) {
            return;
        }
        String hash = hashToken(token);
        // Tránh lưu trùng nếu token này đã nằm trong danh sách revoke còn hiệu lực
        if (!revokedTokenRepository.existsByTokenHashAndExpiresAtAfter(hash, Instant.now())) {
            revokedTokenRepository.save(RevokedToken.builder()
                    .tokenHash(hash)
                    .expiresAt(expiration.toInstant())
                    .build());
        }
        // Dọn dẹp các bản ghi revoke đã hết hạn để giữ bảng gọn
        revokedTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }

    /**
     * Kiểm tra token có nằm trong danh sách đã thu hồi (còn hiệu lực) hay không.
     *
     * @param token chuỗi JWT
     * @return true nếu token đã bị revoke
     */
    public boolean isRevoked(String token) {
        return revokedTokenRepository.existsByTokenHashAndExpiresAtAfter(hashToken(token), Instant.now());
    }

    /**
     * Băm token bằng SHA-256 và chuyển sang chuỗi hex.
     * Lưu hash thay vì token gốc để tránh lưu trữ token nhạy cảm trực tiếp trong DB.
     *
     * @param token chuỗi JWT
     * @return chuỗi hash hex
     */
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
