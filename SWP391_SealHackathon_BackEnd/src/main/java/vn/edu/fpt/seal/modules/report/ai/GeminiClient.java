package vn.edu.fpt.seal.modules.report.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import vn.edu.fpt.seal.config.AiProperties;

import java.util.List;
import java.util.Map;

/**
 * {@link LlmClient} backed by the Google Generative Language API (Gemini),
 * usable with a Google AI Studio key.
 * <p>
 * Endpoint: POST {baseUrl}/v1beta/models/{model}:generateContent?key=API_KEY
 * <p>
 * The key is passed as a query param per Google's spec. We never log the key or
 * the full URL. Any non-2xx / transport / parse problem becomes an
 * {@link LlmException} so the service layer can fall back to code-only stats.
 */
@Slf4j
@Component
public class GeminiClient implements LlmClient {

    private final AiProperties props;
    private final ObjectMapper mapper;
    private final RestClient http;

    public GeminiClient(AiProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getTimeoutMs());
        factory.setReadTimeout(props.getTimeoutMs());

        this.http = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    @Override
    public boolean isAvailable() {
        return props.isUsable() && "gemini".equalsIgnoreCase(props.getProvider());
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            throw new LlmException("Gemini client is not configured (seal.ai.enabled/api-key)");
        }

        // Build request body for generateContent.
        Map<String, Object> body = Map.of(
                "system_instruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))),
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", userPrompt)))),
                "generationConfig", Map.of(
                        "temperature", 0.4,
                        "maxOutputTokens", props.getMaxOutputTokens(),
                        "responseMimeType", "application/json")
        );

        String path = "/v1beta/models/" + props.getModel() + ":generateContent";

        String raw;
        try {
            raw = http.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("key", props.getApiKey())
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            // Do not leak the key-bearing URL; report model + path only.
            log.warn("Gemini call failed (model={}, path={}): {}", props.getModel(), path, e.getMessage());
            throw new LlmException("Gemini request failed: " + e.getMessage(), e);
        }

        return extractText(raw);
    }

    /**
     * Pull candidates[0].content.parts[*].text out of the Gemini response.
     */
    private String extractText(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new LlmException("Empty response from Gemini");
        }
        try {
            JsonNode root = mapper.readTree(raw);
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                // Surface safety blocks / errors instead of silently returning null.
                JsonNode err = root.path("error");
                if (!err.isMissingNode()) {
                    throw new LlmException("Gemini error: " + err.path("message").asText("unknown"));
                }
                throw new LlmException("Gemini returned no candidates");
            }
            JsonNode parts = candidates.get(0).path("content").path("parts");
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : parts) {
                sb.append(part.path("text").asText(""));
            }
            String text = sb.toString().trim();
            if (text.isEmpty()) {
                throw new LlmException("Gemini returned empty text");
            }
            return text;
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("Failed to parse Gemini response: " + e.getMessage(), e);
        }
    }
}
