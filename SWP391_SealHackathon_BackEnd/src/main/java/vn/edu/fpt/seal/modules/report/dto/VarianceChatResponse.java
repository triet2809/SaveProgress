package vn.edu.fpt.seal.modules.report.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record VarianceChatResponse(
        UUID roundId,
        boolean aiAvailable,
        String aiError,
        String reply,
        VarianceAnalysisResponse.Stats stats,
        List<String> suggestions
) {
}
