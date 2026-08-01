package vn.edu.fpt.seal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Config for the AI variance-analysis feature (Gemini via Google AI Studio).
 *
 * Bound from the {@code seal.ai.*} tree. The API key must come from an env var
 * ({@code SEAL_AI_API_KEY}); never hardcode it. When {@code enabled=false} the
 * analysis endpoint returns the code-computed stats only and skips the LLM call
 * so the app still works offline / without a key (e.g. during a demo).
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "seal.ai")
public class AiProperties {
    /** Master switch. If false, no outbound LLM call is made. */
    private boolean enabled = false;
    /** Only "gemini" implemented for now; kept for future providers. */
    private String provider = "gemini";
    /** Gemini model id, e.g. gemini-2.0-flash. */
    private String model = "gemini-2.0-flash";
    /** Google AI Studio API key. Inject via ${SEAL_AI_API_KEY}. */
    private String apiKey = "";
    /** Base endpoint for the Generative Language API. */
    private String baseUrl = "https://generativelanguage.googleapis.com";
    /** HTTP call timeout in milliseconds. */
    private int timeoutMs = 20000;
    /** Upper bound on generated tokens (keeps cost/latency bounded). */
    private int maxOutputTokens = 1500;
    /** Output language for AI narrative. Keep English for UI consistency. */
    private String language = "en";

    /** True only when the feature is on and a key is actually present. */
    public boolean isUsable() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
