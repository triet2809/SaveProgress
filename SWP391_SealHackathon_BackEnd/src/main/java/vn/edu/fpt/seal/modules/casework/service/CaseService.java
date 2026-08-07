package vn.edu.fpt.seal.modules.casework.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.enums.IncidentStatus;
import vn.edu.fpt.seal.common.enums.IncidentType;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.casework.dto.CaseResponse;
import vn.edu.fpt.seal.modules.casework.dto.CreateCaseRequest;
import vn.edu.fpt.seal.modules.casework.dto.UpdateCaseStatusRequest;
import vn.edu.fpt.seal.modules.incident.dto.CreateIncidentRequest;
import vn.edu.fpt.seal.modules.incident.dto.IncidentResponse;
import vn.edu.fpt.seal.modules.incident.dto.UpdateIncidentStatusRequest;
import vn.edu.fpt.seal.modules.incident.entity.IncidentReport;
import vn.edu.fpt.seal.modules.incident.repository.IncidentReportRepository;
import vn.edu.fpt.seal.modules.incident.service.IncidentService;
import vn.edu.fpt.seal.security.AuthorizationService;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Service nghiệp vụ cho "case" (vụ việc).
 * Case là lớp bọc lại IncidentReport: chuyển đổi thuật ngữ case sang incident,
 * uỷ quyền xử lý thực tế cho IncidentService, rồi ánh xạ kết quả ngược lại CaseResponse.
 */
@Service
@RequiredArgsConstructor
public class CaseService {
    private final IncidentReportRepository incidents;
    private final IncidentService incidentService;
    private final AuthorizationService authorizationService;

    /**
     * Liệt kê case có phân trang.
     * Nếu không phải coordinator và không truyền eventId thì chỉ xem case của chính mình (bảo mật phạm vi).
     *
     * @throws ApiException nếu không có eventId lẫn reporterId
     */
    @Transactional(readOnly = true)
    public Page<CaseResponse> list(UUID eventId, UUID reporterId, Pageable pageable, Authentication auth) {
        // Coordinator xem toàn bộ case (mọi sự kiện) khi không truyền eventId; user thường chỉ xem case của chính mình.
        boolean coordinator = hasCoordinator(auth);
        if (eventId == null && !coordinator) reporterId = authorizationService.current(auth).getId();
        Page<IncidentReport> page = eventId != null ? incidents.findByEventId(eventId, pageable)
                : reporterId != null ? incidents.findByReporterId(reporterId, pageable)
                : incidents.findAll(pageable);
        List<CaseResponse> visible = page.stream().map(this::map).toList();
        return new PageImpl<>(visible, pageable, page.getTotalElements());
    }

    /**
     * Lấy chi tiết một case theo id. @throws ApiException nếu không tìm thấy
     */
    @Transactional(readOnly = true)
    public CaseResponse get(UUID id, Authentication auth) {
        IncidentReport incident = incidents.findWithRelationsById(id).orElseThrow(() -> ApiException.notFound("Case not found"));
        return map(incident);
    }

    /**
     * Tạo case mới.
     * Ánh xạ category (thuật ngữ case) sang IncidentType tương ứng, sau đó uỷ quyền cho IncidentService tạo incident.
     *
     * @return CaseResponse với trạng thái mặc định "open"
     */
    @Transactional
    public CaseResponse create(CreateCaseRequest request, Authentication auth) {
        // Chuyển category dạng case sang loại incident nội bộ
        IncidentType type = switch (request.category().toLowerCase(Locale.ROOT)) {
            case "technical_support" -> IncidentType.technical_issue;
            case "rule_question", "conduct_incident" -> IncidentType.rule_violation;
            case "submission_issue", "scoring_issue" -> IncidentType.invalid_submission;
            default -> IncidentType.other;
        };
        IncidentResponse source = incidentService.create(new CreateIncidentRequest(request.eventId(), request.trackId(), request.roundId(),
                request.teamId(), request.submissionId(), type, null, request.category(), request.subject(), request.description()), auth);
        // Đọc lại kèm quan hệ để lấy tên event/track/team hiển thị.
        return incidents.findWithRelationsById(source.id()).map(this::map).orElseGet(() ->
                new CaseResponse("INC-" + source.id(), "incident", request.category(), source.title(), source.description(),
                        "open", source.eventId(), source.roundId(), source.trackId(), source.teamId(), source.submissionId(),
                        source.reporterId(), source.reporterEmail(), null, null, null, source.createdAt(), source.updatedAt()));
    }

    /**
     * Cập nhật trạng thái case.
     * Ánh xạ status dạng case sang IncidentStatus rồi uỷ quyền cho IncidentService.
     *
     * @throws ApiException nếu trạng thái không hợp lệ
     */
    @Transactional
    public CaseResponse updateStatus(UUID id, UpdateCaseStatusRequest request, Authentication auth) {
        // Ánh xạ trạng thái case -> trạng thái incident nội bộ
        IncidentStatus target = switch (request.status().toLowerCase(Locale.ROOT)) {
            case "open" -> IncidentStatus.reported;
            case "in_progress", "awaiting_reporter" -> IncidentStatus.under_review;
            case "resolved", "closed" -> IncidentStatus.resolved;
            case "rejected" -> IncidentStatus.rejected;
            default -> throw ApiException.badRequest("Unknown case status");
        };
        IncidentResponse source = incidentService.updateStatus(id, new UpdateIncidentStatusRequest(target, null, request.note()), auth);
        // Đọc lại kèm quan hệ để trả về tên event/track/team.
        return incidents.findWithRelationsById(source.id()).map(this::map).orElseGet(() ->
                new CaseResponse("INC-" + source.id(), "incident", source.category(), source.title(), source.description(),
                        normalize(source.status()), source.eventId(), source.roundId(), source.trackId(), source.teamId(), source.submissionId(),
                        source.reporterId(), source.reporterEmail(), null, null, null, source.createdAt(), source.updatedAt()));
    }

    /**
     * Chuẩn hóa IncidentStatus nội bộ sang chuỗi trạng thái case hiển thị cho client.
     */
    private String normalize(IncidentStatus status) {
        return switch (status) {
            case reported -> "open";
            case under_review -> "in_progress";
            case resolved -> "resolved";
            case rejected -> "rejected";
        };
    }

    /**
     * Kiểm tra người dùng có quyền COORDINATOR không.
     */
    private boolean hasCoordinator(Authentication a) {
        return a != null && a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_COORDINATOR"));
    }

    /**
     * Ánh xạ IncidentReport sang CaseResponse; xử lý null-safe cho các quan hệ tuỳ chọn.
     */
    private CaseResponse map(IncidentReport i) {
        return new CaseResponse("INC-" + i.getId(), "incident", i.getCategory(), i.getTitle(), i.getDescription(), normalize(i.getStatus()),
                i.getEvent().getId(), i.getRound() == null ? null : i.getRound().getId(), i.getTrack() == null ? null : i.getTrack().getId(),
                i.getTeam() == null ? null : i.getTeam().getId(), i.getSubmission() == null ? null : i.getSubmission().getId(),
                i.getReporter().getId(), i.getReporter().getFullName(),
                i.getEvent().getTitle(), i.getTrack() == null ? null : i.getTrack().getName(), i.getTeam() == null ? null : i.getTeam().getName(),
                i.getCreatedAt(), i.getUpdatedAt());
    }
}
