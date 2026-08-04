package vn.edu.fpt.seal.modules.report.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * AI-assisted inter-judge variance analysis for a round.
 * <p>
 * Design boundary (agreed): the numbers are all computed in code
 * ({@code stats}); the LLM only interprets them into a narrative
 * ({@code ai}). If the LLM is disabled/unavailable, {@code stats} is still
 * returned with {@code aiAvailable=false} and an optional {@code aiError}.
 * <p>
 * Names shown here are the REAL team/judge names (the endpoint is
 * coordinator-only). The data sent to the external LLM is anonymized; the AI
 * narrative is de-anonymized (aliases mapped back to real names) before it
 * reaches this response.
 */
@Builder
public record VarianceAnalysisResponse(
        UUID roundId,
        Stats stats,
        Ai ai,
        boolean aiAvailable,
        String aiError
) {

    /**
     * Purely code-computed statistics (deterministic, reproducible).
     */
    @Builder
    public record Stats(
            int groupCount,
            int highVarianceCount,
            BigDecimal avgVariance,
            List<Hotspot> hotspots,
            List<JudgeBias> judgeBiases
    ) {
    }

    /**
     * One (team, criterion) group where judges disagreed most.
     */
    @Builder
    public record Hotspot(
            UUID teamId,
            String teamName,
            UUID criterionId,
            String criterionName,
            int judgeCount,
            List<BigDecimal> scores,
            BigDecimal meanScore,
            BigDecimal variance,
            BigDecimal stddev,
            BigDecimal minScore,
            BigDecimal maxScore,
            BigDecimal spread,
            /** OUTLIER | POLARIZED | SPREAD | CONSENSUS */
            String pattern,
            /** Real name of the single outlier judge, if the pattern is OUTLIER. */
            String outlierJudge
    ) {
    }

    /**
     * Per-judge tendency across the whole round.
     */
    @Builder
    public record JudgeBias(
            UUID judgeId,
            String judgeName,
            int groupsScored,
            /** Avg of (judge score - group mean); + = lenient, - = harsh. */
            BigDecimal avgDeviation,
            /** LENIENT | HARSH | BALANCED | INCONSISTENT */
            String tendency,
            /** How many groups this judge was the extreme scorer in. */
            int outlierCount
    ) {
    }

    /**
     * LLM-generated narrative. Null when AI is unavailable.
     */
    @Builder
    public record Ai(
            String summary,
            List<String> recommendations,
            List<String> judgeNotes
    ) {
    }
}
