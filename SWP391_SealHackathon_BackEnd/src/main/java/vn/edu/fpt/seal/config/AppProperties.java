package vn.edu.fpt.seal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Nhóm cấu hình ứng dụng ánh xạ từ các thuộc tính có tiền tố {@code app.*}
 * trong file cấu hình (application.yml/properties).
 *
 * <p>Gom các thiết lập bảo mật (JWT), CORS, lời mời (invitation) và văn bản
 * pháp lý (legal) vào một nơi để inject vào các service/config khác.</p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    /** Cấu hình bảo mật (bao gồm JWT). */
    private Security security = new Security();
    /** Cấu hình CORS (danh sách origin được phép). */
    private Cors cors = new Cors();
    /** Cấu hình lời mời tham gia đội (invitation). */
    private Invitation invitation = new Invitation();
    /** Cấu hình văn bản pháp lý (điều khoản, chính sách). */
    private Legal legal = new Legal();

    /** Nhóm cấu hình bảo mật. */
    @Data
    public static class Security {
        /** Cấu hình JWT. */
        private Jwt jwt = new Jwt();
    }

    /** Cấu hình sinh và xác thực JSON Web Token. */
    @Data
    public static class Jwt {
        /** Khóa bí mật dùng để ký/xác minh token (HMAC). */
        private String secret;
        /** Thời hạn access token, tính bằng phút (mặc định 60). */
        private int accessTokenExpirationMinutes = 60;
        /** Thời hạn refresh token, tính bằng ngày (mặc định 7). */
        private int refreshTokenExpirationDays = 7;
        /** Issuer ghi trong token để nhận diện nguồn phát hành. */
        private String issuer = "seal-hackathon";
    }

    /** Cấu hình CORS cho phép gọi API từ frontend. */
    @Data
    public static class Cors {
        /** Danh sách origin được phép truy cập API. */
        private List<String> allowedOrigins = List.of("http://localhost:5173");
    }

    /** Cấu hình liên quan đến lời mời tham gia đội thi. */
    @Data
    public static class Invitation {
        /** Có trả link lời mời trực tiếp trong phản hồi API hay không (hữu ích khi test). */
        private boolean exposeLink = false;
        /** URL gốc của frontend để tạo link lời mời. */
        private String frontendBaseUrl = "http://localhost:5173";
        /** Thời hạn hiệu lực của token lời mời, tính bằng phút. */
        private int tokenExpirationMinutes = 60;
    }

    /** Cấu hình phiên bản và đường dẫn các văn bản pháp lý. */
    @Data
    public static class Legal {
        /** Phiên bản điều khoản sử dụng hiện hành. */
        private String termsVersion = "2026-01";
        /** Phiên bản chính sách bảo mật hiện hành. */
        private String privacyVersion = "2026-01";
        /** Đường dẫn tới điều khoản sử dụng. */
        private String termsUrl = "https://example.invalid/terms";
        /** Đường dẫn tới chính sách bảo mật. */
        private String privacyUrl = "https://example.invalid/privacy";
    }
}
