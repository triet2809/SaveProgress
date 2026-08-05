package vn.edu.fpt.seal.modules.ranking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.enums.PromotionStatus;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.criteria.repository.RoundCriterionRepository;
import vn.edu.fpt.seal.modules.ranking.dto.RecalculateRankingsRequest;
import vn.edu.fpt.seal.modules.ranking.dto.RoundRankingResponse;
import vn.edu.fpt.seal.modules.ranking.entity.RoundRanking;
import vn.edu.fpt.seal.modules.ranking.mapper.RoundRankingMapper;
import vn.edu.fpt.seal.modules.ranking.repository.RoundRankingRepository;
import vn.edu.fpt.seal.modules.recognition.service.TeamRecognitionService;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.team.repository.TeamRepository;
import vn.edu.fpt.seal.security.AuthorizationService;

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
        this.rankingRepository = r;
        this.roundRepository = rounds;
        this.teamRepository = teams;
        this.criterionRepository = criteria;
        this.authorizationService = auth;
        this.lifecycleService = null;
        this.recognitionService = null;
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
        if (weightSum.compareTo(new BigDecimal("100")) != 0) {
            throw ApiException.badRequest(
                    "Criterion weights must sum to 100 before recalculating rankings (current sum: " + weightSum.toPlainString() + ")");
        }
        List<RoundRankingRepository.RoundScoreRow> rows = rankingRepository.calculateRows(roundId);

        List<RoundRankingRepository.RoundScoreRow> ordered = new ArrayList<>(rows);
        ordered.sort(
                Comparator.<RoundRankingRepository.RoundScoreRow, BigDecimal>comparing(
                                r -> nz(r.getTotalScore()), Comparator.reverseOrder())
                        .thenComparing(RoundRankingRepository.RoundScoreRow::getEarliestSubmittedAt,
                                Comparator.nullsLast(Comparator.naturalOrder()))
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
            boolean tiedWithPrev = prevTotal != null && nz(row.getTotalScore()).compareTo(prevTotal) == 0;
            saved.add(rankingRepository.save(RoundRanking.builder()
                    .round(round)
                    .team(team)
                    .totalScore(row.getTotalScore())
                    .rank(rank)
                    .status(status)
                    .tieBreakerReason(tiedWithPrev
                            ? "Tie on total score broken by submission time"
                            : "Ranked by total weighted score")
                    .build()));
            prevTotal = nz(row.getTotalScore());
            rank++;
        }
        var recognitionByTeam = recognitionService == null ? Map.<UUID, List<vn.edu.fpt.seal.modules.recognition.dto.RecognitionDtos.Summary>>of()
                : recognitionService.activeByTeamIds(saved.stream()
                .map(ranking -> ranking.getTeam().getId()).toList());
        return saved.stream().map(ranking -> RoundRankingMapper.toResponse(ranking,
                recognitionByTeam.getOrDefault(ranking.getTeam().getId(), List.of()))).toList();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
