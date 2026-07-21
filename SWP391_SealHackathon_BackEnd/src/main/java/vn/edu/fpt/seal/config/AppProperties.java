package vn.edu.fpt.seal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Security security = new Security();
    private Cors cors = new Cors();
    private Invitation invitation = new Invitation();
    private Legal legal = new Legal();

    @Data
    public static class Security {
        private Jwt jwt = new Jwt();
    }

    @Data
    public static class Jwt {
        private String secret;
        private int accessTokenExpirationMinutes = 60;
        private int refreshTokenExpirationDays = 7;
        private String issuer = "seal-hackathon";
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:5173");
    }

    @Data
    public static class Invitation {
        private boolean exposeLink = false;
        private String frontendBaseUrl = "http://localhost:5173";
        private int tokenExpirationMinutes = 60;
    }

    @Data
    public static class Legal {
        private String termsVersion = "2026-01";
        private String privacyVersion = "2026-01";
        private String termsUrl = "https://example.invalid/terms";
        private String privacyUrl = "https://example.invalid/privacy";
    }
}
