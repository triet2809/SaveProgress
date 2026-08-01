package vn.edu.fpt.seal.modules.report.ai;

/**
 * Provider-agnostic boundary for a single text completion call.
 *
 * Swapping the LLM vendor (Gemini today, OpenAI/local tomorrow) means adding a
 * new implementation, not touching the analysis logic. Implementations should
 * be resilient: a transport/timeout/HTTP error surfaces as {@link LlmException}
 * so callers can fall back to the code-computed stats.
 */
public interface LlmClient {

    /**
     * @param systemPrompt role/behavior instruction for the model
     * @param userPrompt   the actual request payload (pre-computed stats JSON)
     * @return the model's raw text response
     * @throws LlmException on any failure (disabled, timeout, HTTP, parse)
     */
    String complete(String systemPrompt, String userPrompt);

    /** True when the client is configured and allowed to make a call. */
    boolean isAvailable();

    /** Thrown on any LLM failure so the service layer can degrade gracefully. */
    class LlmException extends RuntimeException {
        public LlmException(String message) { super(message); }
        public LlmException(String message, Throwable cause) { super(message, cause); }
    }
}
