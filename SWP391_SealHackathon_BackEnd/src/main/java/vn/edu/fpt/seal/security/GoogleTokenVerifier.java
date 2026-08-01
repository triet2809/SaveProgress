package vn.edu.fpt.seal.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.config.AppProperties;

import java.util.Collections;

/**
 * Xác minh Google ID token (OpenID Connect) phía backend.
 *
 * <p>Kiểm tra chữ ký của Google, thời hạn (exp) và audience (aud) khớp với
 * OAuth Client ID cấu hình. Trả về payload đã xác minh để lấy {@code sub},
 * {@code email}, {@code name}. Đây là bước bắt buộc để tin tưởng token do
 * frontend gửi lên (không tin tưởng dữ liệu client tự khai).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleTokenVerifier {

    private final AppProperties appProperties;

    /**
     * Xác minh Google ID token và trả về payload đã kiểm chứng.
     *
     * @param idToken chuỗi Google ID token do frontend gửi lên
     * @return payload đã xác minh (chứa sub, email, name...)
     * @throws ApiException nếu chưa cấu hình client ID, token không hợp lệ hoặc lỗi xác minh
     */
    public GoogleIdToken.Payload verify(String idToken) {
        String clientId = appProperties.getSecurity().getGoogle().getClientId();
        if (clientId == null || clientId.isBlank()) {
            log.warn("Google client ID is not configured (app.security.google.client-id)");
            throw ApiException.serviceUnavailable("Google sign-in is not configured");
        }

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();

        GoogleIdToken token;
        try {
            token = verifier.verify(idToken);
        } catch (Exception e) {
            // Lỗi mạng, JWK không tải được, token sai định dạng...
            log.warn("Google token verify threw: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            throw ApiException.unauthorized("Could not verify Google token");
        }
        if (token == null) {
            // Token sai chữ ký, sai audience hoặc đã hết hạn
            log.warn("Google token verify returned null (bad signature/audience/expiry). Configured aud={}", clientId);
            throw ApiException.unauthorized("Invalid Google token");
        }
        return token.getPayload();
    }
}
