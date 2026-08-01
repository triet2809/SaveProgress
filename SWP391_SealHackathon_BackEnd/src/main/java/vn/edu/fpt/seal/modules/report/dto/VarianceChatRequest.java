package vn.edu.fpt.seal.modules.report.dto;

import java.util.List;
import java.util.UUID;

/**
 * Chat request for variance-analysis assistant.
 */
public record VarianceChatRequest(
        UUID roundId,
        UUID eventId,
        UUID trackId,
        List<Message> messages,
        boolean refresh
) {
    public record Message(String role, String content) {}
}
