package vn.edu.fpt.seal.modules.report.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Anonymized scoring dataset export (requirement #12).
 * <p>
 * Team and judge identities are replaced with stable pseudonyms ("Team 1",
 * "Judge A") so the data can be shared for analysis/research without exposing
 * who scored whom or which real team is which. The mapping is NOT included on
 * purpose. Criterion names are kept because they carry no personal data and
 * are needed to interpret the scores.
 */
@Builder
public record AnonymizedDatasetResponse(
        UUID roundId,
        int teamCount,
        int judgeCount,
        List<Row> rows
) {
    @Builder
    public record Row(
            String teamAlias,
            String judgeAlias,
            String criterionName,
            BigDecimal criterionWeight,
            BigDecimal score,
            BigDecimal weightedScore
    ) {
    }
}
