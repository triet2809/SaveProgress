package vn.edu.fpt.seal.modules.report.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Inter-judge variance dashboard (requirement #13).
 * <p>
 * One row per (team, criterion) pair in a round, reporting how much the
 * assigned judges disagreed: the number of judges, their mean score,
 * population variance and standard deviation, the min/max and the spread
 * (max - min). High variance flags a criterion/team where judges saw things
 * very differently and a coordinator may want to review.
 */
@Builder
public record JudgeVarianceResponse(
        UUID teamId,
        String teamName,
        UUID trackId,
        String trackName,
        UUID criterionId,
        String criterionName,
        int judgeCount,
        List<BigDecimal> scores,
        BigDecimal meanScore,
        BigDecimal variance,
        BigDecimal stddev,
        BigDecimal minScore,
        BigDecimal maxScore,
        BigDecimal spread
) {
}
