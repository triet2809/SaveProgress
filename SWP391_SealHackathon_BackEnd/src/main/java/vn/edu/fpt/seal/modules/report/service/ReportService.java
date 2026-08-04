package vn.edu.fpt.seal.modules.report.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.ranking.dto.RoundRankingResponse;
import vn.edu.fpt.seal.modules.ranking.mapper.RoundRankingMapper;
import vn.edu.fpt.seal.modules.ranking.repository.RoundRankingRepository;
import vn.edu.fpt.seal.modules.report.dto.AnonymizedDatasetResponse;
import vn.edu.fpt.seal.modules.report.dto.JudgeVarianceResponse;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;
import vn.edu.fpt.seal.modules.score.repository.ScoreRepository;
import vn.edu.fpt.seal.modules.score.repository.ScoreRepository.RoundScoreDetailRow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Read-only reporting/analytics over a round's scores:
 * - #11 ranking CSV export
 * - #12 anonymized dataset export
 * - #13 inter-judge variance dashboard
 * <p>
 * All three derive from the same raw score rows so the numbers are consistent
 * with what the ranking endpoint produces.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ScoreRepository scoreRepository;
    private final RoundRankingRepository rankingRepository;
    private final RoundRepository roundRepository;

    private Round requireRound(UUID roundId) {
        return roundRepository.findById(roundId)
                .orElseThrow(() -> ApiException.notFound("Round not found: " + roundId));
    }

    // ---- #13 inter-judge variance -----------------------------------------

    /**
     * For each (team, criterion) pair, compute mean / variance / stddev across
     * the judges who scored it, plus the spread (max-min). High variance flags
     * a criterion where judges disagreed and may need a calibration discussion.
     */
    @Transactional(readOnly = true)
    public List<JudgeVarianceResponse> judgeVariance(UUID eventId, UUID roundId, UUID trackId) {
        Round round = requireRound(roundId);
        if (!round.getTrack().getEvent().getId().equals(eventId))
            throw ApiException.badRequest("Round does not belong to the selected event");
        if (trackId != null && !round.getTrack().getId().equals(trackId))
            throw ApiException.badRequest("Track is not relevant to the selected round");
        List<RoundScoreDetailRow> rows = scoreRepository.findRoundScoreDetails(roundId, trackId);

        // group raw scores by team+criterion
        Map<String, List<RoundScoreDetailRow>> groups = new LinkedHashMap<>();
        for (RoundScoreDetailRow r : rows) {
            String key = r.getTeamId() + "|" + r.getCriterionId();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }

        List<JudgeVarianceResponse> out = new ArrayList<>();
        for (List<RoundScoreDetailRow> g : groups.values()) {
            RoundScoreDetailRow head = g.get(0);
            List<BigDecimal> scores = g.stream().map(RoundScoreDetailRow::getScore).toList();
            int n = scores.size();

            double mean = scores.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
            double variance = n == 0 ? 0 :
                    scores.stream().mapToDouble(s -> Math.pow(s.doubleValue() - mean, 2)).sum() / n;
            double stddev = Math.sqrt(variance);
            double min = scores.stream().mapToDouble(BigDecimal::doubleValue).min().orElse(0);
            double max = scores.stream().mapToDouble(BigDecimal::doubleValue).max().orElse(0);

            out.add(JudgeVarianceResponse.builder()
                    .teamId(head.getTeamId())
                    .teamName(head.getTeamName())
                    .trackId(head.getTrackId())
                    .trackName(head.getTrackName())
                    .criterionId(head.getCriterionId())
                    .criterionName(head.getCriterionName())
                    .judgeCount(n)
                    .scores(scores)
                    .meanScore(round2(mean))
                    .variance(round4(variance))
                    .stddev(round4(stddev))
                    .minScore(round2(min))
                    .maxScore(round2(max))
                    .spread(round2(max - min))
                    .build());
        }
        // Highest-disagreement first so a coordinator sees problem areas on top.
        out.sort(Comparator.comparing(JudgeVarianceResponse::variance).reversed());
        return out;
    }

    // ---- #12 anonymized dataset -------------------------------------------

    @Transactional(readOnly = true)
    public AnonymizedDatasetResponse anonymizedDataset(UUID roundId) {
        requireRound(roundId);
        List<RoundScoreDetailRow> rows = scoreRepository.findRoundScoreDetails(roundId, null);

        // Assign stable aliases in first-seen order.
        Map<UUID, String> teamAlias = new LinkedHashMap<>();
        Map<UUID, String> judgeAlias = new LinkedHashMap<>();
        for (RoundScoreDetailRow r : rows) {
            teamAlias.computeIfAbsent(r.getTeamId(), k -> "Team " + (teamAlias.size() + 1));
            judgeAlias.computeIfAbsent(r.getJudgeId(), k -> "Judge " + alpha(judgeAlias.size()));
        }

        List<AnonymizedDatasetResponse.Row> outRows = rows.stream()
                .map(r -> AnonymizedDatasetResponse.Row.builder()
                        .teamAlias(teamAlias.get(r.getTeamId()))
                        .judgeAlias(judgeAlias.get(r.getJudgeId()))
                        .criterionName(r.getCriterionName())
                        .criterionWeight(r.getCriterionWeight())
                        .score(r.getScore())
                        .weightedScore(r.getWeightedScore())
                        .build())
                .toList();

        return AnonymizedDatasetResponse.builder()
                .roundId(roundId)
                .teamCount(teamAlias.size())
                .judgeCount(judgeAlias.size())
                .rows(outRows)
                .build();
    }

    // ---- #11 ranking CSV ---------------------------------------------------

    /**
     * Render the current persisted rankings of a round as CSV. Reads whatever
     * the last recalculate produced (does not recompute), so export reflects the
     * official standings.
     */
    @Transactional(readOnly = true)
    public String rankingCsv(UUID roundId) {
        requireRound(roundId);
        List<RoundRankingResponse> rankings = rankingRepository
                .findByRoundId(roundId, org.springframework.data.domain.Pageable.unpaged())
                .map(RoundRankingMapper::toResponse)
                .stream()
                .sorted(Comparator.comparing(r -> r.rank() == null ? Integer.MAX_VALUE : r.rank()))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("rank,team,totalScore,status,tieBreakerCriterion,tieBreakerScore\n");
        for (RoundRankingResponse r : rankings) {
            sb.append(r.rank() == null ? "" : r.rank()).append(',')
                    .append(csv(r.teamName())).append(',')
                    .append(r.totalScore() == null ? "" : r.totalScore()).append(',')
                    .append(r.status() == null ? "" : r.status().name()).append(',')
                    .append(r.tieBreakerCriterionId() == null ? "" : r.tieBreakerCriterionId()).append(',')
                    .append(r.tieBreakerScore() == null ? "" : r.tieBreakerScore()).append('\n');
        }
        return sb.toString();
    }

    // ---- helpers -----------------------------------------------------------

    /**
     * RFC-4180-ish CSV escaping: quote when the value has comma/quote/newline.
     */
    private static String csv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            return '"' + v.replace("\"", "\"\"") + '"';
        }
        return v;
    }

    /**
     * 0 -> A, 1 -> B ... 25 -> Z, 26 -> AA ...
     */
    private static String alpha(int idx) {
        StringBuilder sb = new StringBuilder();
        int n = idx;
        do {
            sb.insert(0, (char) ('A' + (n % 26)));
            n = n / 26 - 1;
        } while (n >= 0);
        return sb.toString();
    }

    private static BigDecimal round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal round4(double v) {
        return BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP);
    }
}
