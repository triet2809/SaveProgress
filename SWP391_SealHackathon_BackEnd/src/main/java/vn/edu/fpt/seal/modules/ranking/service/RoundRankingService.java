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
        boolean applyPromotion = req == null || !Boolean.FALSE.equals(req.applyPromotion());
        if (lifecycleService != null) lifecycleService.requireRankingRecalculationAllowed(round);
        BigDecimal weightSum = criterionRepository.sumWeightByRoundId(roundId);
        if (weightSum.compareTo(new BigDecimal("100")) != 0) {
            throw ApiException.badRequest(
                    "Criterion weights must sum to 100 before recalculating rankings (current sum: " + weightSum.toPlainString() + ")");
        }
        List<RoundRankingRepository.RoundScoreRow> rows = rankingRepository.calculateRows(roundId);

        // Xây dựng bảng tra cứu điểm từng tiêu chí cho mỗi đội để phục vụ phân hạng khi hòa.
        TieBreaker tieBreaker = new TieBreaker(rankingRepository.criterionScores(roundId));

        // Thứ tự ưu tiên phân hạng (áp dụng tuần tự):
        //   1. Tổng điểm có trọng số — cao hơn xếp trước (logic gốc, không đổi).
        //   2. Nếu bằng tổng điểm → so tiêu chí theo trọng số giảm dần (logic gốc, không đổi).
        //   3. Nếu mọi tiêu chí cũng bằng → đội nộp bài SỚM HƠN xếp trước (bổ sung mới).
        //   4. Tên đội A–Z để kết quả luôn xác định.
        List<RoundRankingRepository.RoundScoreRow> ordered = new ArrayList<>(rows);
        ordered.sort(
                Comparator.<RoundRankingRepository.RoundScoreRow, BigDecimal>comparing(
                                r -> nz(r.getTotalScore()), Comparator.reverseOrder())
                        .thenComparing(tieBreaker::compareByCriterion)
                        .thenComparing(RoundRankingRepository.RoundScoreRow::getEarliestSubmittedAt,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(r -> r.getTeamName() == null ? "" : r.getTeamName()));

        rankingRepository.deleteByRoundId(roundId);
        rankingRepository.flush();

        int topN = round.getTopNToPromote() == null ? 0 : round.getTopNToPromote();
        List<RoundRanking> saved = new ArrayList<>();
        int rank = 1;
        BigDecimal prevTotal = null;
        UUID prevTeamId = null;
        for (RoundRankingRepository.RoundScoreRow row : ordered) {
            Team team = teamRepository.findById(row.getTeamId()).orElseThrow(() -> ApiException.notFound("Team not found: " + row.getTeamId()));
            PromotionStatus status = PromotionStatus.pending;
            if (applyPromotion) status = rank <= topN ? PromotionStatus.promoted : PromotionStatus.eliminated;

            // Kiểm tra đội hiện tại có hòa tổng điểm với đội xếp ngay trước không.
            boolean tiedWithPrev = prevTotal != null && nz(row.getTotalScore()).compareTo(prevTotal) == 0;
            // Nếu hòa → tìm tiêu chí đầu tiên mà hai đội khác nhau để ghi nhận lý do.
            // decisive == null có nghĩa mọi tiêu chí bằng nhau → thời gian nộp bài đã phân hạng.
            TieBreaker.Decisive decisive = (tiedWithPrev && prevTeamId != null)
                    ? tieBreaker.decisiveBetween(prevTeamId, row.getTeamId())
                    : null;

            RoundRanking.RoundRankingBuilder builder = RoundRanking.builder()
                    .round(round)
                    .team(team)
                    .totalScore(row.getTotalScore())
                    .rank(rank)
                    .status(status);
            if (decisive != null) {
                // Hòa tổng điểm nhưng khác điểm ở một tiêu chí → ghi nhận tiêu chí đó.
                builder.tieBreakerCriterion(criterionRepository.getReferenceById(decisive.criterionId()))
                        .tieBreakerScore(decisive.score())
                        .tieBreakerReason("Tie on total score broken by highest-weight criterion '" + decisive.criterionName() + "'");
            } else if (tiedWithPrev) {
                // Hòa cả tổng điểm lẫn từng tiêu chí → thời gian nộp bài sớm hơn đã quyết định.
                builder.tieBreakerReason("Tie on total score and all criteria equal; resolved by earliest submission time");
            } else {
                builder.tieBreakerReason("Ranked by total weighted score; team name used for deterministic ordering on ties");
            }
            saved.add(rankingRepository.save(builder.build()));
            prevTeamId = row.getTeamId();
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

    /**
     * Lớp hỗ trợ phá hòa khi nhiều đội có cùng tổng điểm.
     *
     * Cách hoạt động:
     *   - Nhận dữ liệu điểm từng tiêu chí của từng đội (query criterionScores trả về,
     *     đã sắp xếp theo trọng số giảm dần).
     *   - Lưu vào bảng tra cứu nội bộ: teamId → (criterionId → điểm có trọng số).
     *
     * Hai phương thức chính:
     *   - compareByCriterion: so sánh hai đội theo từng tiêu chí (logic GỐC, không đổi).
     *   - decisiveBetween: xác định tiêu chí đầu tiên mà hai đội khác nhau, hoặc trả về
     *     null nếu mọi tiêu chí bằng nhau — báo hiệu thời gian nộp bài là yếu tố quyết định.
     */
    static final class TieBreaker {
        // Danh sách tiêu chí sắp xếp theo trọng số giảm dần (tiêu chí quan trọng nhất đứng đầu).
        private final List<CriterionRef> criteriaByWeightDesc = new ArrayList<>();
        // Bảng tra cứu nhanh: teamId → (criterionId → điểm có trọng số của đội đó).
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

        /**
         * So sánh hai đội theo từng tiêu chí, ưu tiên tiêu chí có trọng số cao nhất.
         * Trả về số âm nếu a xếp trước b, dương nếu b xếp trước, 0 nếu mọi tiêu chí bằng nhau.
         * Phương thức này KHÔNG thay đổi so với phiên bản gốc.
         */
        int compareByCriterion(RoundRankingRepository.RoundScoreRow a, RoundRankingRepository.RoundScoreRow b) {
            for (CriterionRef c : criteriaByWeightDesc) {
                BigDecimal sa = scoreOf(a.getTeamId(), c.id());
                BigDecimal sb = scoreOf(b.getTeamId(), c.id());
                int cmp = sb.compareTo(sa); // desc
                if (cmp != 0) return cmp;
            }
            return 0;
        }

        /**
         * Tìm tiêu chí đầu tiên (theo trọng số giảm dần) mà teamA và teamB có điểm khác nhau.
         * Trả về null nếu tất cả tiêu chí đều bằng nhau → nghĩa là thời gian nộp bài
         * (earliestSubmittedAt) đã là yếu tố phân hạng cuối cùng giữa hai đội.
         * Điểm được lưu là điểm của teamB (đội xếp sau) trên tiêu chí quyết định đó.
         * Đây là phương thức mới, thay thế decisiveFor() cũ vốn chỉ xét một đội đơn lẻ
         * mà không so sánh trực tiếp với đội đứng trước — kém chính xác hơn.
         */
        Decisive decisiveBetween(UUID teamA, UUID teamB) {
            for (CriterionRef c : criteriaByWeightDesc) {
                BigDecimal sa = scoreOf(teamA, c.id());
                BigDecimal sb = scoreOf(teamB, c.id());
                if (sa.compareTo(sb) != 0) return new Decisive(c.id(), c.name(), sb);
            }
            return null;
        }

        private BigDecimal scoreOf(UUID teamId, UUID criterionId) {
            Map<UUID, BigDecimal> m = byTeam.get(teamId);
            if (m == null) return BigDecimal.ZERO;
            return m.getOrDefault(criterionId, BigDecimal.ZERO);
        }

        record CriterionRef(UUID id, String name, BigDecimal weight) {
        }

        record Decisive(UUID criterionId, String criterionName, BigDecimal score) {
        }
    }
}
