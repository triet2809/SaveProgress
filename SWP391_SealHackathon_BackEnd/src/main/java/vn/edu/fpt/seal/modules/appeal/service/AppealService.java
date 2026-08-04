package vn.edu.fpt.seal.modules.appeal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.enums.RoundLifecycleState;
import vn.edu.fpt.seal.common.enums.TeamMemberRole;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.appeal.dto.AppealDtos;
import vn.edu.fpt.seal.modules.appeal.entity.Appeal;
import vn.edu.fpt.seal.modules.appeal.repository.AppealRepository;
import vn.edu.fpt.seal.modules.participant.repository.RoundParticipantRepository;
import vn.edu.fpt.seal.modules.resultversion.repository.RoundResultVersionRepository;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;
import vn.edu.fpt.seal.modules.round.service.CompetitionLifecycleService;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.team.entity.TeamMember;
import vn.edu.fpt.seal.modules.team.repository.TeamMemberRepository;
import vn.edu.fpt.seal.modules.timeline.TimelineEventType;
import vn.edu.fpt.seal.modules.timeline.TimelineScope;
import vn.edu.fpt.seal.modules.timeline.TimelineSourceType;
import vn.edu.fpt.seal.modules.timeline.dto.TimelineEventRequest;
import vn.edu.fpt.seal.modules.timeline.service.TimelineService;
import vn.edu.fpt.seal.modules.user.entity.User;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.security.CurrentUser;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service xử lý nghiệp vụ khiếu nại kết quả.
 * Bao gồm: tạo khiếu nại (với kiểm tra quyền đội trưởng, hạn chót, trùng lặp),
 * phản hồi, giải quyết, và ghi các sự kiện timeline tương ứng.
 * Khi khiếu nại được tạo, vòng thi chuyển sang trạng thái PAUSED_FOR_APPEAL.
 */
@Service
@RequiredArgsConstructor
public class AppealService {
    private final AppealRepository appeals;
    private final RoundRepository rounds;
    private final RoundResultVersionRepository versions;
    private final RoundParticipantRepository participants;
    private final TeamMemberRepository members;
    private final UserRepository users;
    private final CompetitionLifecycleService lifecycle;
    private final Clock clock;
    @Autowired
    private TimelineService timeline;

    /**
     * Tạo mới một khiếu nại.
     * Kiểm tra: người dùng phải là đội trưởng của sự kiện chứa vòng thi; đội phải tham gia vòng;
     * phải có phiên bản kết quả đã "published"; chưa quá hạn khiếu nại; chưa khiếu nại trùng.
     * Sau khi tạo, chuyển vòng thi sang PAUSED_FOR_APPEAL và ghi timeline.
     *
     * @param req  dữ liệu tạo (roundId, reason)
     * @param auth thông tin xác thực người dùng hiện tại
     * @return DTO khiếu nại vừa tạo
     * @throws ApiException nếu vi phạm quyền, hạn chót hoặc trùng lặp
     */
    @Transactional
    public AppealDtos.Response create(AppealDtos.Create req, Authentication auth) {
        CurrentUser c = current(auth);
        Round r = rounds.findById(req.roundId()).orElseThrow(() -> ApiException.notFound("Round not found"));
        // Tìm bản ghi đội trưởng của người dùng ứng với sự kiện của vòng thi; chỉ đội trưởng được khiếu nại
        TeamMember leader = members.findByUserIdOrderByJoinedAtDesc(c.getId()).stream().filter(m -> m.getRole() == TeamMemberRole.leader && m.getTeam().getTrack().getEvent().getId().equals(r.getTrack().getEvent().getId())).findFirst().orElseThrow(() -> ApiException.forbidden("Only the current event team leader may appeal"));
        Team team = leader.getTeam();
        if (!participants.existsByRoundIdAndTeamId(r.getId(), team.getId()))
            throw ApiException.forbidden("Team did not participate in this round");
        // Phải tồn tại phiên bản kết quả đang công bố (published) để có thể khiếu nại
        var v = versions.findByRoundIdAndStatus(r.getId(), "published").orElseThrow(() -> ApiException.badRequest("No active published result version"));
        // Chặn khiếu nại sau hạn chót
        LocalDateTime now = LocalDateTime.now(clock);
        if (!now.isBefore(v.getAppealDeadline())) throw ApiException.badRequest("Appeal deadline has expired");
        // Chặn khiếu nại trùng cho cùng một phiên bản kết quả
        if (appeals.existsByTeamIdAndResultVersionId(team.getId(), v.getId()))
            throw ApiException.conflict("Team already appealed this result version");
        User user = users.findById(c.getId()).orElseThrow();
        Appeal a = appeals.save(Appeal.builder().event(r.getTrack().getEvent()).round(r).team(team).submittedBy(user).resultVersion(v)
                .reason(req.reason().trim()).status("PENDING").resultPublishedAt(v.getPublishedAt()).appealDeadline(v.getAppealDeadline()).createdAt(now).build());
        // Tạm dừng tiến trình vòng thi trong khi chờ xử lý khiếu nại
        r.setLifecycleState(RoundLifecycleState.PAUSED_FOR_APPEAL);
        record(a, TimelineEventType.APPEAL_SUBMITTED, TimelineScope.TEAM_PRIVATE, "Appeal submitted", "The team submitted an appeal", "submitted");
        record(a, TimelineEventType.ROUND_PAUSED_FOR_APPEAL, TimelineScope.EVENT_PARTICIPANTS, "Competition paused for appeal", "Round progression is paused during appeal review", "pause");
        return map(a);
    }

    /**
     * Liệt kê khiếu nại theo sự kiện, lọc theo trạng thái (nếu có). @throws ApiException nếu thiếu eventId
     */
    @Transactional
    public List<AppealDtos.Response> list(UUID eventId, String status) {
        if (eventId == null) throw ApiException.badRequest("eventId is required");
        return appeals.findByEventIdOrderByCreatedAtDesc(eventId).stream().filter(a -> status == null || a.getStatus().equalsIgnoreCase(status)).map(this::map).toList();
    }

    /**
     * Lấy khiếu nại của một đội; coordinator xem được tất cả, thành viên chỉ xem đội mình. @throws ApiException nếu ngoài phạm vi
     */
    @Transactional
    public List<AppealDtos.Response> team(UUID teamId, Authentication auth) {
        CurrentUser c = current(auth);
        boolean coordinator = c.getRoles().contains("coordinator");
        if (!coordinator && !members.existsByTeamIdAndUserId(teamId, c.getId()))
            throw ApiException.forbidden("Out of scope");
        return appeals.findByTeamIdOrderByCreatedAtDesc(teamId).stream().map(this::map).toList();
    }

    /**
     * Lấy chi tiết khiếu nại; kiểm tra quyền xem (coordinator hoặc thành viên đội). @throws ApiException nếu ngoài phạm vi
     */
    @Transactional
    public AppealDtos.Response get(UUID id, Authentication auth) {
        Appeal a = find(id);
        CurrentUser c = current(auth);
        if (!c.getRoles().contains("coordinator") && !members.existsByTeamIdAndUserId(a.getTeam().getId(), c.getId()))
            throw ApiException.forbidden("Out of scope");
        return map(a);
    }

    /**
     * Ghi phản hồi/yêu cầu làm rõ; chỉ áp dụng khi khiếu nại còn PENDING. @throws ApiException nếu đã giải quyết
     */
    @Transactional
    public AppealDtos.Response respond(UUID id, AppealDtos.Respond req) {
        Appeal a = find(id);
        if (!a.getStatus().equals("PENDING")) throw ApiException.conflict("Appeal already resolved");
        a.setResponse(req.response().trim());
        a.setUpdatedAt(LocalDateTime.now(clock));
        record(a, TimelineEventType.APPEAL_CLARIFICATION_REQUESTED, TimelineScope.TEAM_PRIVATE, "Appeal clarification requested", "The coordinator requested clarification", "clarification");
        return map(a);
    }

    /**
     * Giải quyết khiếu nại: chấp nhận hoặc từ chối.
     * Chỉ xử lý khiếu nại đang PENDING; quyết định phải là ACCEPTED hoặc REJECTED.
     * Nếu ACCEPTED kèm recalculationRequired thì đánh dấu cần tính lại kết quả.
     * Cập nhật vòng đời vòng thi qua lifecycle và ghi các sự kiện timeline.
     *
     * @throws ApiException nếu đã giải quyết hoặc quyết định không hợp lệ
     */
    @Transactional
    public AppealDtos.Response resolve(UUID id, AppealDtos.Resolve req, Authentication auth) {
        Appeal a = find(id);
        if (!a.getStatus().equals("PENDING")) throw ApiException.conflict("Appeal already resolved");
        String d = req.status().toUpperCase();
        if (!Set.of("REJECTED", "ACCEPTED").contains(d))
            throw ApiException.badRequest("Decision must be ACCEPTED or REJECTED");
        boolean recalc = d.equals("ACCEPTED") && Boolean.TRUE.equals(req.recalculationRequired());
        a.setStatus(d);
        a.setDecision(d);
        a.setRecalculationRequired(recalc);
        a.setResponse(req.response());
        a.setResolvedBy(users.findById(current(auth).getId()).orElseThrow());
        a.setResolvedAt(LocalDateTime.now(clock));
        lifecycle.resolved(a.getRound(), recalc);
        TimelineEventType type = d.equals("REJECTED") ? TimelineEventType.APPEAL_REJECTED : recalc ? TimelineEventType.APPEAL_ACCEPTED_RECALCULATION : TimelineEventType.APPEAL_ACCEPTED;
        record(a, type, TimelineScope.TEAM_PRIVATE, d.equals("REJECTED") ? "Appeal rejected" : "Appeal accepted", "The appeal decision is available", "resolved:" + d + ":" + recalc);
        record(a, TimelineEventType.APPEAL_RESOLVED, TimelineScope.TEAM_PRIVATE, "Appeal resolved", "The appeal review is complete", "resolved");
        if (recalc)
            record(a, TimelineEventType.RESULT_RECALCULATION_REQUIRED, TimelineScope.COORDINATOR_PRIVATE, "Result recalculation required", "A result version requires controlled recalculation", "recalculation");
        return map(a);
    }

    /**
     * Tìm khiếu nại kèm quan hệ; ném 404 nếu không tồn tại.
     */
    private Appeal find(UUID id) {
        return appeals.findWithRelationsById(id).orElseThrow(() -> ApiException.notFound("Appeal not found"));
    }

    /**
     * Ghi một sự kiện timeline gắn với khiếu nại; bỏ qua nếu timeline chưa được inject. Phạm vi EVENT_PARTICIPANTS thì không gắn teamId.
     */
    private void record(Appeal a, TimelineEventType type, TimelineScope scope, String title, String description, String suffix) {
        if (timeline == null) return;
        timeline.record(new TimelineEventRequest(a.getEvent().getId(), scope == TimelineScope.EVENT_PARTICIPANTS ? null : a.getTeam().getId(), a.getRound().getId(), a.getRound().getTrack().getId(), type, TimelineSourceType.APPEAL, a.getId(), scope, title, description, null, "appeal:" + a.getId() + ":" + suffix));
    }

    /**
     * Lấy CurrentUser từ Authentication; ném 401 nếu chưa xác thực.
     */
    private CurrentUser current(Authentication a) {
        if (a == null || !(a.getPrincipal() instanceof CurrentUser c))
            throw ApiException.unauthorized("Authentication required");
        return c;
    }

    /**
     * Ánh xạ thực thể Appeal sang DTO Response; tính số giây còn lại để khiếu nại và làm mới vòng đời vòng thi.
     */
    private AppealDtos.Response map(Appeal a) {
        lifecycle.refresh(a.getRound());
        long remaining = a.getAppealDeadline() == null ? 0 : Math.max(0, Duration.between(LocalDateTime.now(clock), a.getAppealDeadline()).getSeconds());
        Integer version = a.getResultVersion() == null ? null : a.getResultVersion().getVersionNumber();
        return new AppealDtos.Response(a.getId(), a.getEvent().getId(), a.getRound().getId(), a.getRound().getName(), a.getTeam().getId(), a.getTeam().getName(), a.getSubmittedBy().getId(), a.getReason(), a.getStatus(), a.getResponse(), a.getDecision(), a.isRecalculationRequired(), version, a.getResultPublishedAt(), a.getAppealDeadline(), a.getCreatedAt(), a.getResolvedAt(), a.getRound().getLifecycleState().name(), remaining);
    }
}
