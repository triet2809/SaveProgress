package vn.edu.fpt.seal.modules.report.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.report.dto.VarianceAnalysisResponse.Hotspot;
import vn.edu.fpt.seal.modules.report.dto.VarianceAnalysisResponse.JudgeBias;
import vn.edu.fpt.seal.modules.report.dto.VarianceAnalysisResponse.Stats;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;
import vn.edu.fpt.seal.modules.score.repository.ScoreRepository;
import vn.edu.fpt.seal.modules.score.repository.ScoreRepository.RoundScoreDetailRow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Computes the quantitative layer of the variance analysis, entirely in code
 * (deterministic, reproducible, cheap). The LLM never recomputes these numbers;
 * it only interprets them.
 *
 * On top of the plain per-(team,criterion) variance it adds two things the raw
 * numbers hide:
 *   - pattern classification: is a high variance caused by ONE outlier judge,
 *     a POLARIZED split, or a general SPREAD?
 *   - per-judge bias across the whole round: is a judge systematically LENIENT
 *     or HARSH vs. their peers, or just INCONSISTENT?
 */
@Service
@RequiredArgsConstructor
public class VarianceStatsService {

    /** Variance at/above this is flagged as "high" (matches the FE threshold). */
    public static final double HIGH_VARIANCE = 10.0;
    /** A score this many stddevs from the mean is a candidate outlier. */
    private static final double OUTLIER_Z = 1.5;
    /** Avg deviation beyond this (points) marks a judge lenient/harsh. */
    private static final double BIAS_POINTS = 1.0;
    /** How many hotspots to surface (highest variance first). */
    private static final int MAX_HOTSPOTS = 15;

    private final ScoreRepository scoreRepository;
    private final RoundRepository roundRepository;

    @Transactional(readOnly = true)
    public Stats compute(UUID eventId, UUID roundId, UUID trackId) {
        var round = roundRepository.findById(roundId)
                .orElseThrow(() -> ApiException.notFound("Round not found: " + roundId));
        if (!round.getTrack().getEvent().getId().equals(eventId)) throw ApiException.badRequest("Round does not belong to the selected event");
        if (trackId != null && !round.getTrack().getId().equals(trackId)) throw ApiException.badRequest("Track is not relevant to the selected round");
        List<RoundScoreDetailRow> rows = scoreRepository.findRoundScoreDetails(roundId, trackId);

        // Group raw scores by team+criterion.
        Map<String, List<RoundScoreDetailRow>> groups = new LinkedHashMap<>();
        for (RoundScoreDetailRow r : rows) {
            groups.computeIfAbsent(r.getTeamId() + "|" + r.getCriterionId(), k -> new ArrayList<>()).add(r);
        }

        List<Hotspot> hotspots = new ArrayList<>();
        // Accumulators for per-judge bias across the whole round.
        Map<UUID, JudgeAcc> judgeAcc = new LinkedHashMap<>();

        double varianceSum = 0;
        int highCount = 0;

        for (List<RoundScoreDetailRow> g : groups.values()) {
            RoundScoreDetailRow head = g.get(0);
            List<BigDecimal> scores = g.stream().map(RoundScoreDetailRow::getScore).toList();
            int n = scores.size();

            double mean = scores.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
            double variance = n == 0 ? 0
                    : scores.stream().mapToDouble(s -> Math.pow(s.doubleValue() - mean, 2)).sum() / n;
            double stddev = Math.sqrt(variance);
            double min = scores.stream().mapToDouble(BigDecimal::doubleValue).min().orElse(0);
            double max = scores.stream().mapToDouble(BigDecimal::doubleValue).max().orElse(0);

            varianceSum += variance;
            if (variance >= HIGH_VARIANCE) highCount++;

            // --- pattern + outlier judge ---
            String pattern;
            String outlierJudge = null;
            if (variance < HIGH_VARIANCE) {
                pattern = "CONSENSUS";
            } else {
                RoundScoreDetailRow ext = findOutlier(g, mean, stddev);
                if (ext != null) {
                    pattern = "OUTLIER";
                    outlierJudge = ext.getJudgeName();
                } else if (isPolarized(scores, mean)) {
                    pattern = "POLARIZED";
                } else {
                    pattern = "SPREAD";
                }
            }

            // --- feed per-judge bias accumulators ---
            for (RoundScoreDetailRow r : g) {
                JudgeAcc acc = judgeAcc.computeIfAbsent(r.getJudgeId(),
                        k -> new JudgeAcc(r.getJudgeName()));
                acc.deviations.add(r.getScore().doubleValue() - mean);
                if (outlierJudge != null && r.getJudgeName().equals(outlierJudge)) {
                    acc.outlierCount++;
                }
            }

            hotspots.add(Hotspot.builder()
                    .teamId(head.getTeamId())
                    .teamName(head.getTeamName())
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
                    .pattern(pattern)
                    .outlierJudge(outlierJudge)
                    .build());
        }

        // Highest-disagreement first, keep only the top N for the AI prompt.
        hotspots.sort(Comparator.comparing(Hotspot::variance).reversed());
        List<Hotspot> topHotspots = hotspots.size() > MAX_HOTSPOTS
                ? new ArrayList<>(hotspots.subList(0, MAX_HOTSPOTS))
                : hotspots;

        List<JudgeBias> judgeBiases = buildJudgeBiases(judgeAcc);

        return Stats.builder()
                .groupCount(groups.size())
                .highVarianceCount(highCount)
                .avgVariance(groups.isEmpty() ? BigDecimal.ZERO : round4(varianceSum / groups.size()))
                .hotspots(topHotspots)
                .judgeBiases(judgeBiases)
                .build();
    }

    /**
     * Returns the single outlier row if one score dominates the disagreement:
     * its |deviation| is the largest, exceeds OUTLIER_Z * stddev, and removing
     * it cuts the variance by more than half. Otherwise null.
     */
    private RoundScoreDetailRow findOutlier(List<RoundScoreDetailRow> g, double mean, double stddev) {
        if (g.size() < 3 || stddev == 0) return null;

        RoundScoreDetailRow extreme = null;
        double maxAbsDev = -1;
        for (RoundScoreDetailRow r : g) {
            double dev = Math.abs(r.getScore().doubleValue() - mean);
            if (dev > maxAbsDev) { maxAbsDev = dev; extreme = r; }
        }
        if (extreme == null || maxAbsDev < OUTLIER_Z * stddev) return null;

        // Variance of the remaining scores after dropping the extreme one.
        List<Double> rest = new ArrayList<>();
        boolean dropped = false;
        for (RoundScoreDetailRow r : g) {
            if (!dropped && r == extreme) { dropped = true; continue; }
            rest.add(r.getScore().doubleValue());
        }
        double restMean = rest.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double restVar = rest.stream().mapToDouble(s -> Math.pow(s - restMean, 2)).sum() / rest.size();
        double fullVar = stddev * stddev;

        return restVar <= 0.5 * fullVar ? extreme : null;
    }

    /**
     * Polarized = scores fall into a low cluster and a high cluster with a clear
     * gap around the mean and roughly balanced sides (no single outlier).
     */
    private boolean isPolarized(List<BigDecimal> scores, double mean) {
        int below = 0, above = 0;
        double maxBelow = Double.NEGATIVE_INFINITY, minAbove = Double.POSITIVE_INFINITY;
        for (BigDecimal s : scores) {
            double v = s.doubleValue();
            if (v < mean) { below++; maxBelow = Math.max(maxBelow, v); }
            else if (v > mean) { above++; minAbove = Math.min(minAbove, v); }
        }
        if (below < 2 || above < 2) return false;
        // A visible gap separating the two clusters.
        return (minAbove - maxBelow) >= 2.0;
    }

    private List<JudgeBias> buildJudgeBiases(Map<UUID, JudgeAcc> judgeAcc) {
        List<JudgeBias> out = new ArrayList<>();
        for (Map.Entry<UUID, JudgeAcc> e : judgeAcc.entrySet()) {
            JudgeAcc acc = e.getValue();
            int cnt = acc.deviations.size();
            double avgDev = acc.deviations.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double devVar = cnt == 0 ? 0
                    : acc.deviations.stream().mapToDouble(d -> Math.pow(d - avgDev, 2)).sum() / cnt;
            double devStd = Math.sqrt(devVar);

            String tendency;
            if (avgDev >= BIAS_POINTS) tendency = "LENIENT";
            else if (avgDev <= -BIAS_POINTS) tendency = "HARSH";
            else if (devStd >= 2.0) tendency = "INCONSISTENT";
            else tendency = "BALANCED";

            out.add(JudgeBias.builder()
                    .judgeId(e.getKey())
                    .judgeName(acc.name)
                    .groupsScored(cnt)
                    .avgDeviation(round2(avgDev))
                    .tendency(tendency)
                    .outlierCount(acc.outlierCount)
                    .build());
        }
        // Most systematically biased first (largest |avgDeviation|).
        out.sort(Comparator.comparingDouble(
                (JudgeBias b) -> Math.abs(b.avgDeviation().doubleValue())).reversed());
        return out;
    }

    private static final class JudgeAcc {
        final String name;
        final List<Double> deviations = new ArrayList<>();
        int outlierCount = 0;
        JudgeAcc(String name) { this.name = name; }
    }

    private static BigDecimal round2(double v) { return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP); }
    private static BigDecimal round4(double v) { return BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP); }
}
