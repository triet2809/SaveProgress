package vn.edu.fpt.seal.modules.ranking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import vn.edu.fpt.seal.common.enums.PromotionStatus;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.criteria.repository.RoundCriterionRepository;
import vn.edu.fpt.seal.modules.ranking.dto.*;
import vn.edu.fpt.seal.modules.ranking.entity.RoundRanking;
import vn.edu.fpt.seal.modules.ranking.mapper.RoundRankingMapper;
import vn.edu.fpt.seal.modules.ranking.repository.RoundRankingRepository;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.team.repository.TeamRepository;
import vn.edu.fpt.seal.security.AuthorizationService;
import vn.edu.fpt.seal.modules.recognition.service.TeamRecognitionService;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class RoundRankingService {
    private final RoundRankingRepository rankingRepository;
    private final RoundRepository roundRepository;
    private final TeamRepository teamRepository;
    private final RoundCriterionRepository criterionRepository;
    private final AuthorizationService authorizationService;
    private final vn.edu.fpt.seal.modules.round.service.CompetitionLifecycleService lifecycleService;
    private final TeamRecognitionService recognitionService;

    public RoundRankingService(RoundRankingRepository r, RoundRepository rounds, TeamRepository teams,
                               RoundCriterionRepository criteria, AuthorizationService auth) {
        this.rankingRepository=r; this.roundRepository=rounds; this.teamRepository=teams;
        this.criterionRepository=criteria; this.authorizationService=auth; this.lifecycleService=null;
        this.recognitionService=null;
    }

    @Transactional(readOnly = true)
    public Page<RoundRankingResponse> list(UUID eventId, UUID roundId, UUID trackId, Pageable pageable, Authentication authentication) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> ApiException.notFound("Round not found: " + roundId));
        if (eventId != null && !round.getTrack().getEvent().getId().equals(eventId)) {
            throw ApiException.badRequest("Round does not belong to the selected event");
        }
        if (trackId != null && !round.getTrack().getId().equals(trackId)) {
            throw ApiException.badRequest("Track is not relevant to the selected round");
        }
        authorizationService.require(
                authorizationService.canReadRanking(authorizationService.current(authentication), round),
                "Round rankings have not been published or are outside your assignment scope");
        Pageable effectivePageable = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("rank").ascending());
        Page<RoundRanking> rankings = rankingRepository.findByRoundId(roundId, effectivePageable);
        var recognitionByTeam = recognitionService == null ? Map.<UUID, List<vn.edu.fpt.seal.modules.recognition.dto.RecognitionDtos.Summary>>of()
                : recognitionService.activeByTeamIds(rankings.getContent().stream()
                .map(ranking -> ranking.getTeam().getId()).toList());
        return rankings.map(ranking -> RoundRankingMapper.toResponse(ranking,
                recognitionByTeam.getOrDefault(ranking.getTeam().getId(), List.of())));
    }

    @Transactional
    public List<RoundRankingResponse> recalculate(UUID roundId, RecalculateRankingsRequest req) {
        Round round = roundRepository.findById(roundId).orElseThrow(() -> ApiException.notFound("Round not found: " + roundId));
        boolean applyPromotion = req != null && Boolean.TRUE.equals(req.applyPromotion());
        if (lifecycleService != null) lifecycleService.requireRankingRecalculationAllowed(round);
        BigDecimal weightSum = criterionRepository.sumWeightByRoundId(roundId);
        if (weightSum.compareTo(BigDecimal.ONE) != 0) {
            throw ApiException.badRequest(
                    "Criterion weights must sum to 1.0 before recalculating rankings (current sum: " + weightSum.toPlainString() + ")");
        }
        List<RoundRankingRepository.RoundScoreRow> rows = rankingRepository.calculateRows(roundId);

        // Requirement #8: build a per-team, per-criterion lookup so ties on the
        // total weighted score can be broken by the highest-weight criterion.
        TieBreaker tieBreaker = new TieBreaker(rankingRepository.criterionScores(roundId));

        // Re-order the (already total-desc, name-asc) rows so that teams tied on
        // total are ordered by their score on the highest-weight criterion first.
        List<RoundRankingRepository.RoundScoreRow> ordered = new ArrayList<>(rows);
        ordered.sort(
                Comparator.<RoundRankingRepository.RoundScoreRow, BigDecimal>comparing(
                                r -> nz(r.getTotalScore()), Comparator.reverseOrder())
                        .thenComparing(tieBreaker::compareByCriterion)
                        .thenComparing(r -> r.getTeamName() == null ? "" : r.getTeamName()));

        rankingRepository.deleteByRoundId(roundId);
        rankingRepository.flush();

        int topN = round.getTopNToPromote() == null ? 0 : round.getTopNToPromote();
        List<RoundRanking> saved = new ArrayList<>();
        int rank = 1;
        BigDecimal prevTotal = null;
        for (RoundRankingRepository.RoundScoreRow row : ordered) {
            Team team = teamRepository.findById(row.getTeamId()).orElseThrow(() -> ApiException.notFound("Team not found: " + row.getTeamId()));
            PromotionStatus status = PromotionStatus.pending;
            if (applyPromotion) status = rank <= topN ? PromotionStatus.promoted : PromotionStatus.eliminated;

            // Populate tie-breaker fields only when this team was actually tied on
            // total with the previous (higher-ranked) team, so the data shows WHY
            // the order is what it is.
            boolean tiedWithPrev = prevTotal != null && nz(row.getTotalScore()).compareTo(prevTotal) == 0;
            TieBreaker.Decisive decisive = tiedWithPrev ? tieBreaker.decisiveFor(row.getTeamId()) : null;

            RoundRanking.RoundRankingBuilder builder = RoundRanking.builder()
                    .round(round)
                    .team(team)
                    .totalScore(row.getTotalScore())
                    .rank(rank)
                    .status(status);
            if (decisive != null) {
                builder.tieBreakerCriterion(criterionRepository.getReferenceById(decisive.criterionId()))
                        .tieBreakerScore(decisive.score())
                        .tieBreakerReason("Tie on total score broken by highest-weight criterion '" + decisive.criterionName() + "'");
            } else {
                builder.tieBreakerReason("Ranked by total weighted score; team name used for deterministic ordering on ties");
            }
            saved.add(rankingRepository.save(builder.build()));
            prevTotal = nz(row.getTotalScore());
            rank++;
        }
        var recognitionByTeam = recognitionService == null ? Map.<UUID, List<vn.edu.fpt.seal.modules.recognition.dto.RecognitionDtos.Summary>>of()
                : recognitionService.activeByTeamIds(saved.stream()
                .map(ranking -> ranking.getTeam().getId()).toList());
        return saved.stream().map(ranking -> RoundRankingMapper.toResponse(ranking,
                recognitionByTeam.getOrDefault(ranking.getTeam().getId(), List.of()))).toList();
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    /**
     * Helper that, given the per-team/per-criterion weighted scores for a round
     * (already ordered by criterion weight desc), can compare two teams by their
     * scores on the most important criteria, and report which criterion was
     * decisive for a given team.
     */
    static final class TieBreaker {
        /** Ordered (weight desc) distinct criteria seen in the round. */
        private final List<CriterionRef> criteriaByWeightDesc = new ArrayList<>();
        /** teamId -> (criterionId -> weighted score on that criterion). */
        private final Map<UUID, Map<UUID, BigDecimal>> byTeam = new HashMap<>();

        TieBreaker(List<RoundRankingRepository.TeamCriterionScoreRow> rows) {
            Set<UUID> seen = new HashSet<>();
            for (RoundRankingRepository.TeamCriterionScoreRow row : rows) {
                // rows are weight desc, name asc -> first time we see a criterion id
                // it is in the correct priority position.
                if (seen.add(row.getCriterionId())) {
                    criteriaByWeightDesc.add(new CriterionRef(row.getCriterionId(), row.getCriterionName(), nz(row.getCriterionWeight())));
                }
                byTeam.computeIfAbsent(row.getTeamId(), k -> new HashMap<>())
                        .put(row.getCriterionId(), nz(row.getCriterionScore()));
            }
        }

        /** Higher score on the highest-weight criterion comes first (negative). */
        int compareByCriterion(RoundRankingRepository.RoundScoreRow a, RoundRankingRepository.RoundScoreRow b) {
            for (CriterionRef c : criteriaByWeightDesc) {
                BigDecimal sa = scoreOf(a.getTeamId(), c.id());
                BigDecimal sb = scoreOf(b.getTeamId(), c.id());
                int cmp = sb.compareTo(sa); // desc
                if (cmp != 0) return cmp;
            }
            return 0;
        }

        /** The first criterion (by weight) on which this team has any score. */
        Decisive decisiveFor(UUID teamId) {
            for (CriterionRef c : criteriaByWeightDesc) {
                BigDecimal s = scoreOf(teamId, c.id());
                if (s.signum() != 0) return new Decisive(c.id(), c.name(), s);
            }
            return null;
        }

        private BigDecimal scoreOf(UUID teamId, UUID criterionId) {
            Map<UUID, BigDecimal> m = byTeam.get(teamId);
            if (m == null) return BigDecimal.ZERO;
            return m.getOrDefault(criterionId, BigDecimal.ZERO);
        }

        record CriterionRef(UUID id, String name, BigDecimal weight) {}
        record Decisive(UUID criterionId, String criterionName, BigDecimal score) {}
    }
}
