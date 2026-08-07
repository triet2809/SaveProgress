package vn.edu.fpt.seal.modules.round.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.enums.RoundLifecycleState;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.appeal.repository.AppealRepository;
import vn.edu.fpt.seal.modules.participant.repository.RoundParticipantRepository;
import vn.edu.fpt.seal.modules.ranking.repository.RoundRankingRepository;
import vn.edu.fpt.seal.modules.resultversion.entity.RoundResultVersion;
import vn.edu.fpt.seal.modules.resultversion.entity.RoundResultVersionEntry;
import vn.edu.fpt.seal.modules.resultversion.repository.RoundResultVersionEntryRepository;
import vn.edu.fpt.seal.modules.resultversion.repository.RoundResultVersionRepository;
import vn.edu.fpt.seal.modules.round.entity.LogicalRoundPromotion;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.round.repository.LogicalRoundPromotionRepository;
import vn.edu.fpt.seal.modules.round.repository.RoundDefinitionRepository;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;
import vn.edu.fpt.seal.modules.user.entity.User;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompetitionLifecycleService {
    /*
     * Service điều phối lifecycle của round sau khi công bố kết quả.
     * Đây là nơi xử lý mở appeal, chờ hết hạn, resume, advance và chặn awards khi chưa đủ điều kiện.
     */
    private final RoundRepository rounds;
    private final AppealRepository appeals;
    private final RoundResultVersionRepository versions;
    private final RoundResultVersionEntryRepository entries;
    private final RoundRankingRepository rankings;
    private final RoundParticipantRepository participants;
    private final Clock clock;

    @Autowired(required = false)
    private LogicalRoundPromotionRepository promotions;

    @Autowired(required = false)
    private RoundDefinitionRepository definitions;

    @Transactional
    public RoundResultVersion publish(Round round, User actor, String reason) {
        // Khi publish: đóng version cũ, tạo version mới, snapshot ranking hiện tại, mở appeal window 15 phút.
        LocalDateTime now = LocalDateTime.now(clock);
        versions.findByRoundIdAndStatus(round.getId(), "published").ifPresent(v -> v.setStatus("superseded"));
        int n = versions.findTopByRoundIdOrderByVersionNumberDesc(round.getId()).map(v -> v.getVersionNumber() + 1).orElse(1);
        RoundResultVersion source = versions.findTopByRoundIdOrderByVersionNumberDesc(round.getId()).orElse(null);
        RoundResultVersion v = versions.save(RoundResultVersion.builder().round(round).versionNumber(n).status("published").publishedAt(now)
                .appealDeadline(now.minusSeconds(1)).publishedBy(actor).sourceVersion(source).reason(reason).createdAt(now).build());
        rankings.findByRoundId(round.getId(), org.springframework.data.domain.Pageable.unpaged()).forEach(r -> entries.save(
                RoundResultVersionEntry.builder()
                        .resultVersion(v)
                        .team(r.getTeam())
                        .rank(r.getRank())
                        .totalScore(r.getTotalScore())
                        .promotionStatus(r.getStatus().name())
                        .tieBreakerScore(r.getTieBreakerScore())
                        .tieBreakerReason(r.getTieBreakerReason())
                        .createdAt(now)
                        .build()));
        round.setResultPublishedAt(now);
        round.setAppealDeadline(now.minusSeconds(1));
        setState(round, RoundLifecycleState.APPEAL_WINDOW_OPEN);
        return v;
    }

    @Transactional
    public Round refresh(Round r) {
        // Refresh lifecycle theo thời gian thực: nếu hết hạn appeal thì chuyển sang READY_TO_ADVANCE hoặc READY_FOR_AWARDS.
        if (r.getLifecycleState() == RoundLifecycleState.APPEAL_WINDOW_OPEN
                && r.getAppealDeadline() != null
                && !LocalDateTime.now(clock).isBefore(r.getAppealDeadline())) {
            setState(r, isFinal(r) ? RoundLifecycleState.READY_FOR_AWARDS : RoundLifecycleState.READY_TO_ADVANCE);
        } else if (r.getLifecycleState() == RoundLifecycleState.PAUSED_FOR_APPEAL
                && r.getAppealDeadline() != null
                && !LocalDateTime.now(clock).isBefore(r.getAppealDeadline())
                && !appeals.existsByRoundIdAndStatus(r.getId(), "PENDING")) {
            setState(r, isFinal(r) ? RoundLifecycleState.READY_FOR_AWARDS : RoundLifecycleState.READY_TO_ADVANCE);
        }
        return r;
    }

    public boolean hasPublishedVersion(UUID roundId) {
        // Kiểm tra round đã có published result version chưa.
        return versions.existsByRoundIdAndStatus(roundId, "published");
    }

    public long remainingSeconds(Round r) {
        // Số giây còn lại trước khi hết appeal deadline để FE hiện countdown.
        return r.getAppealDeadline() == null ? 0 : Math.max(0, Duration.between(LocalDateTime.now(clock), r.getAppealDeadline()).getSeconds());
    }

    @Transactional
    public void resolved(Round r, boolean recalc) {
        // Sau khi xử lý appeal: nếu cần tính lại ranking thì chuyển sang AWAITING_RECALCULATION, ngược lại tạm pause chờ hết hạn.
        if (recalc) {
            setState(r, RoundLifecycleState.AWAITING_RECALCULATION);
            return;
        }
        if (!appeals.existsByRoundIdAndStatus(r.getId(), "PENDING")) {
            setState(r, RoundLifecycleState.PAUSED_FOR_APPEAL);
        }
    }

    @Transactional
    public Round resume(Round r) {
        // Resume round sau appeal, nhưng chỉ khi không còn pending appeal và đã qua trạng thái chặn.
        if (appeals.existsByRoundIdAndStatus(r.getId(), "PENDING")) {
            throw ApiException.conflict("Pending appeals block resume");
        }
        refresh(r);
        if (r.getLifecycleState() == RoundLifecycleState.PAUSED_FOR_APPEAL) {
            throw ApiException.conflict("Appeal deadline has not closed");
        }
        if (!Set.of(RoundLifecycleState.APPEAL_WINDOW_OPEN, RoundLifecycleState.READY_TO_ADVANCE, RoundLifecycleState.READY_FOR_AWARDS).contains(r.getLifecycleState())) {
            throw ApiException.conflict("Round is not ready to resume");
        }
        return r;
    }

    public void requireProgressionAllowed(Round r) {
        // Chặn các thao tác tiến trình nếu round còn bị khóa bởi lifecycle appeal.
        refresh(r);
        if (Set.of(RoundLifecycleState.APPEAL_WINDOW_OPEN, RoundLifecycleState.PAUSED_FOR_APPEAL, RoundLifecycleState.AWAITING_RECALCULATION).contains(r.getLifecycleState())) {
            throw ApiException.conflict("Round progression is blocked by the appeal lifecycle");
        }
    }

    public void requireRankingRecalculationAllowed(Round r) {
        refresh(r);
        // Dùng trạng thái của execution round (không dùng logical round) để SQL reset trên bảng rounds
        // là đủ để mở lại recalculation mà không cần reset thêm bảng round_definitions.
        RoundLifecycleState state = r.getLifecycleState();
        if (state == RoundLifecycleState.ADVANCED) {
            throw ApiException.conflict("Cannot recalculate rankings after round has been advanced");
        }
        // Recalculation cho phép từ mọi trạng thái trừ ADVANCED để coordinator có thể sửa ranking trước khi advance.
        if (Set.of(RoundLifecycleState.READY_TO_ADVANCE, RoundLifecycleState.READY_FOR_AWARDS,
                RoundLifecycleState.APPEAL_WINDOW_OPEN, RoundLifecycleState.PAUSED_FOR_APPEAL).contains(state)) {
            setState(r, RoundLifecycleState.AWAITING_RECALCULATION);
        }
    }

    public void requireAwardsAllowed(UUID eventId) {
        // Trước khi complete event, xác nhận final round đã sẵn sàng trao giải và không còn pending appeal.
        boolean finalRoundFound = false;
        for (Round r : rounds.findByTrackEventId(eventId, org.springframework.data.domain.Pageable.unpaged())) {
            refresh(r);
            if (isFinal(r)) {
                finalRoundFound = true;
                if (r.getLifecycleState() != RoundLifecycleState.READY_FOR_AWARDS) {
                    throw ApiException.conflict("Final round is not ready for awards");
                }
            }
            if (appeals.existsByRoundIdAndStatus(r.getId(), "PENDING")) {
                throw ApiException.conflict("Pending appeals block awards");
            }
        }
        if (!finalRoundFound) {
            throw ApiException.conflict("Event has no final round ready for awards");
        }
    }

    @Transactional
    public Round advance(Round r) {
        // Advance logical round: lấy tất cả team promotion từ published result version và seed sang logical round kế tiếp.
        refresh(r);
        if (r.getLifecycleState() != RoundLifecycleState.READY_TO_ADVANCE) {
            throw ApiException.conflict("Round is not ready to advance");
        }
        if (r.getLogicalRound() == null) {
            // Simple round without logical-round grouping: just mark as ADVANCED.
            setState(r, RoundLifecycleState.ADVANCED);
            return r;
        }
        if (promotions == null || definitions == null) {
            throw ApiException.conflict("Logical-round promotion infrastructure is unavailable");
        }
        var source = r.getLogicalRound();
        var target = definitions.findByEventIdAndSequenceNumber(source.getEvent().getId(), source.getSequenceNumber() + 1)
                .orElseThrow(() -> ApiException.notFound("Next logical round not found"));
        for (Round execution : rounds.findByLogicalRoundId(source.getId())) {
            refresh(execution);
            if (execution.getLifecycleState() != RoundLifecycleState.READY_TO_ADVANCE) {
                throw ApiException.conflict("Every track execution must be ready before logical-round advancement");
            }
            if (appeals.existsByRoundIdAndStatus(execution.getId(), "PENDING")) {
                throw ApiException.conflict("Pending appeals block advancement");
            }
            RoundResultVersion v = versions.findByRoundIdAndStatus(execution.getId(), "published")
                    .orElseThrow(() -> ApiException.badRequest("Every track execution requires a published result version"));
            entries.findByResultVersionId(v.getId()).stream()
                    .filter(e -> "promoted".equals(e.getPromotionStatus()))
                    .forEach(e -> {
                        if (!promotions.existsByTargetLogicalRoundIdAndTeamId(target.getId(), e.getTeam().getId())) {
                            promotions.save(LogicalRoundPromotion.builder()
                                    .sourceLogicalRound(source)
                                    .targetLogicalRound(target)
                                    .team(e.getTeam())
                                    .sourceResultVersionId(v.getId())
                                    .sourceResultEntryId(e.getId())
                                    .build());
                        }
                    });
        }
        setState(r, RoundLifecycleState.ADVANCED);
        return r;
    }

    private boolean isFinal(Round r) {
        // Xác định round hiện tại có phải final round hay không.
        return r.getLogicalRound() != null
                ? r.getLogicalRound().isFinalRound()
                : rounds.findTopByTrackIdOrderBySequenceNumberDesc(r.getTrack().getId()).map(x -> x.getId().equals(r.getId())).orElse(true);
    }

    private void setState(Round round, RoundLifecycleState state) {
        // Đồng bộ lifecycle state cho cả logical round và các track execution con.
        if (round.getLogicalRound() == null) {
            round.setLifecycleState(state);
            return;
        }
        round.getLogicalRound().setLifecycleState(state);
        rounds.findByLogicalRoundId(round.getLogicalRound().getId()).forEach(execution -> execution.setLifecycleState(state));
    }
}
