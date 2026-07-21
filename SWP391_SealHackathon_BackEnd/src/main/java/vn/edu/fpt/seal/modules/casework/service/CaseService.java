package vn.edu.fpt.seal.modules.casework.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.enums.*;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.casework.dto.*;
import vn.edu.fpt.seal.modules.incident.dto.*;
import vn.edu.fpt.seal.modules.incident.entity.IncidentReport;
import vn.edu.fpt.seal.modules.incident.repository.IncidentReportRepository;
import vn.edu.fpt.seal.modules.incident.service.IncidentService;
import vn.edu.fpt.seal.security.AuthorizationService;
import java.util.*;

@Service @RequiredArgsConstructor
public class CaseService {
    private final IncidentReportRepository incidents;
    private final IncidentService incidentService;
    private final AuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public Page<CaseResponse> list(UUID eventId, UUID reporterId, Pageable pageable, Authentication auth) {
        if (eventId == null && !hasCoordinator(auth)) reporterId = authorizationService.current(auth).getId();
        if (eventId == null && reporterId == null) throw ApiException.badRequest("eventId is required");
        Page<IncidentReport> page = eventId != null ? incidents.findByEventId(eventId, pageable)
                : incidents.findByReporterId(reporterId, pageable);
        List<CaseResponse> visible = page.stream().map(this::map).toList();
        return new PageImpl<>(visible, pageable, visible.size());
    }

    @Transactional(readOnly = true)
    public CaseResponse get(UUID id, Authentication auth) {
        IncidentReport incident = incidents.findWithRelationsById(id).orElseThrow(() -> ApiException.notFound("Case not found"));
        return map(incident);
    }

    @Transactional
    public CaseResponse create(CreateCaseRequest request, Authentication auth) {
        IncidentType type = switch (request.category().toLowerCase(Locale.ROOT)) {
            case "technical_support" -> IncidentType.technical_issue;
            case "rule_question", "conduct_incident" -> IncidentType.rule_violation;
            case "submission_issue", "scoring_issue" -> IncidentType.invalid_submission;
            default -> IncidentType.other;
        };
        IncidentResponse source = incidentService.create(new CreateIncidentRequest(request.eventId(), request.trackId(), request.roundId(),
                request.teamId(), request.submissionId(), type, null, request.category(), request.subject(), request.description()), auth);
        return new CaseResponse("INC-" + source.id(), "incident", request.category(), source.title(), source.description(),
                "open", source.eventId(), source.roundId(), source.trackId(), source.teamId(), source.submissionId(),
                source.reporterId(), source.reporterEmail(), source.createdAt(), source.updatedAt());
    }

    @Transactional
    public CaseResponse updateStatus(UUID id, UpdateCaseStatusRequest request, Authentication auth) {
        IncidentStatus target = switch (request.status().toLowerCase(Locale.ROOT)) {
            case "open" -> IncidentStatus.reported;
            case "in_progress", "awaiting_reporter" -> IncidentStatus.under_review;
            case "resolved", "closed" -> IncidentStatus.resolved;
            case "rejected" -> IncidentStatus.rejected;
            default -> throw ApiException.badRequest("Unknown case status");
        };
        IncidentResponse source = incidentService.updateStatus(id, new UpdateIncidentStatusRequest(target, null, request.note()), auth);
        return new CaseResponse("INC-" + source.id(), "incident", source.category(), source.title(), source.description(),
                normalize(source.status()), source.eventId(), source.roundId(), source.trackId(), source.teamId(), source.submissionId(),
                source.reporterId(), source.reporterEmail(), source.createdAt(), source.updatedAt());
    }

    private String normalize(IncidentStatus status) {
        return switch (status) { case reported -> "open"; case under_review -> "in_progress"; case resolved -> "resolved"; case rejected -> "rejected"; };
    }
    private boolean hasCoordinator(Authentication a) { return a != null && a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_COORDINATOR")); }
    private CaseResponse map(IncidentReport i) {
        return new CaseResponse("INC-" + i.getId(), "incident", i.getCategory(), i.getTitle(), i.getDescription(), normalize(i.getStatus()),
                i.getEvent().getId(), i.getRound() == null ? null : i.getRound().getId(), i.getTrack() == null ? null : i.getTrack().getId(),
                i.getTeam() == null ? null : i.getTeam().getId(), i.getSubmission() == null ? null : i.getSubmission().getId(),
                i.getReporter().getId(), i.getReporter().getFullName(), i.getCreatedAt(), i.getUpdatedAt());
    }
}
