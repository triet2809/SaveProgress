package vn.edu.fpt.seal.modules.incident.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.enums.IncidentActionType;
import vn.edu.fpt.seal.common.enums.IncidentStatus;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.event.repository.EventRepository;
import vn.edu.fpt.seal.modules.incident.dto.*;
import vn.edu.fpt.seal.modules.incident.entity.IncidentAction;
import vn.edu.fpt.seal.modules.incident.entity.IncidentReport;
import vn.edu.fpt.seal.modules.incident.mapper.IncidentMapper;
import vn.edu.fpt.seal.modules.incident.repository.IncidentActionRepository;
import vn.edu.fpt.seal.modules.incident.repository.IncidentEvidenceRepository;
import vn.edu.fpt.seal.modules.incident.repository.IncidentReportRepository;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;
import vn.edu.fpt.seal.modules.submission.entity.Submission;
import vn.edu.fpt.seal.modules.submission.repository.SubmissionRepository;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.team.repository.TeamRepository;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.track.repository.TrackRepository;
import vn.edu.fpt.seal.modules.user.entity.User;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.security.AuthorizationService;
import vn.edu.fpt.seal.security.CurrentUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import vn.edu.fpt.seal.modules.timeline.*;
import vn.edu.fpt.seal.modules.timeline.dto.TimelineEventRequest;
import vn.edu.fpt.seal.modules.timeline.service.TimelineService;

@Service
@RequiredArgsConstructor
public class IncidentService {
    @Autowired private TimelineService timeline;
    private final IncidentReportRepository reportRepository;
    private final IncidentEvidenceRepository evidenceRepository;
    private final IncidentActionRepository actionRepository;
    private final EventRepository eventRepository;
    private final TrackRepository trackRepository;
    private final RoundRepository roundRepository;
    private final TeamRepository teamRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public Page<IncidentResponse> list(UUID eventId, UUID reporterId, IncidentStatus status,
                                       Pageable pageable, Authentication authentication) {
        CurrentUser user = authorizationService.current(authentication);
        if (!authorizationService.isCoordinator(user)
                && reporterId != null && !reporterId.equals(user.getId())) {
            throw ApiException.forbidden("You cannot list another reporter's incidents");
        }
        Page<IncidentReport> source = eventId != null
                ? reportRepository.findByEventId(eventId, pageable)
                : reporterId != null
                    ? reportRepository.findByReporterId(reporterId, pageable)
                    : status != null
                        ? reportRepository.findByStatus(status, pageable)
                        : reportRepository.findAll(pageable);
        List<IncidentResponse> visible = source.stream()
                .filter(incident -> authorizationService.canReadIncident(user, incident))
                .map(this::toResponse)
                .toList();
        return new PageImpl<>(visible, pageable, visible.size());
    }

    @Transactional(readOnly = true)
    public IncidentResponse get(UUID id, Authentication authentication) {
        IncidentReport incident = find(id);
        authorizationService.require(
                authorizationService.canReadIncident(authorizationService.current(authentication), incident),
                "You are not authorized to view this incident");
        return toResponse(incident);
    }

    @Transactional
    public IncidentResponse create(CreateIncidentRequest request, Authentication authentication) {
        User reporter = currentUser(authentication);
        Event event = eventRepository.findById(request.eventId())
                .orElseThrow(() -> ApiException.notFound("Event not found: " + request.eventId()));
        Track track = request.trackId() == null ? null : trackRepository.findById(request.trackId())
                .orElseThrow(() -> ApiException.notFound("Track not found: " + request.trackId()));
        Round round = request.roundId() == null ? null : roundRepository.findById(request.roundId())
                .orElseThrow(() -> ApiException.notFound("Round not found: " + request.roundId()));
        Team team = request.teamId() == null ? null : teamRepository.findWithTrackById(request.teamId())
                .orElseThrow(() -> ApiException.notFound("Team not found: " + request.teamId()));
        Submission submission = request.submissionId() == null ? null
                : submissionRepository.findWithRelationsById(request.submissionId())
                    .orElseThrow(() -> ApiException.notFound("Submission not found: " + request.submissionId()));

        validateHierarchy(event, track, round, team, submission);

        IncidentReport incident = IncidentReport.builder()
                .event(event).track(track).round(round).team(team).submission(submission)
                .reporter(reporter).type(request.type()).status(IncidentStatus.reported)
                .severity(trim(request.severity())).category(trim(request.category()))
                .title(request.title().trim()).description(request.description().trim())
                .build();
        incident = reportRepository.save(incident);
        record(incident, TimelineEventType.INCIDENT_SUBMITTED, "Incident submitted", "A staff incident report was submitted", "submitted");
        return toResponse(incident);
    }

    @Transactional
    public IncidentResponse updateStatus(UUID id, UpdateIncidentStatusRequest request, Authentication authentication) {
        IncidentReport incident = find(id);
        IncidentStatus old = incident.getStatus();
        incident.setStatus(request.status());
        if (request.assignedCoordinatorId() != null) {
            incident.setAssignedCoordinator(userRepository.findById(request.assignedCoordinatorId())
                    .orElseThrow(() -> ApiException.notFound("Coordinator not found: " + request.assignedCoordinatorId())));
        }
        if (request.status() == IncidentStatus.resolved || request.status() == IncidentStatus.rejected) {
            incident.setResolvedAt(LocalDateTime.now());
        }
        actionRepository.save(IncidentAction.builder()
                .incident(incident).actionBy(currentUser(authentication))
                .actionType(request.status() == IncidentStatus.rejected
                        ? IncidentActionType.reject_report : IncidentActionType.other)
                .oldValue(old.name()).newValue(request.status().name()).note(request.note()).build());
        TimelineEventType type = request.status() == IncidentStatus.resolved
                ? TimelineEventType.INCIDENT_RESOLVED
                : request.status() == IncidentStatus.rejected
                    ? TimelineEventType.INCIDENT_DISMISSED
                    : TimelineEventType.INCIDENT_UNDER_REVIEW;
        record(incident, type, type == TimelineEventType.INCIDENT_RESOLVED ? "Incident resolved"
                        : type == TimelineEventType.INCIDENT_DISMISSED ? "Incident dismissed" : "Incident under review",
                type == TimelineEventType.INCIDENT_RESOLVED ? "The incident review was closed"
                        : type == TimelineEventType.INCIDENT_DISMISSED ? "The incident report was dismissed"
                        : "The incident is under staff review",
                "status:" + request.status());
        return toResponse(incident);
    }

    @Transactional
    public IncidentResponse addEvidence(UUID id, AddIncidentEvidenceRequest request, Authentication authentication) {
        IncidentReport incident = find(id);
        CurrentUser user = authorizationService.current(authentication);
        authorizationService.require(authorizationService.canReadIncident(user, incident),
                "You are not authorized to add evidence to this incident");
        if ((request.fileUrl() == null || request.fileUrl().isBlank())
                && (request.externalUrl() == null || request.externalUrl().isBlank())) {
            throw ApiException.badRequest("Either fileUrl or externalUrl is required");
        }
        evidenceRepository.save(vn.edu.fpt.seal.modules.incident.entity.IncidentEvidence.builder()
                .incident(incident).uploadedBy(currentUser(authentication))
                .fileUrl(trim(request.fileUrl())).externalUrl(trim(request.externalUrl()))
                .description(trim(request.description())).build());
        return toResponse(incident);
    }

    @Transactional
    public IncidentResponse addAction(UUID id, AddIncidentActionRequest request, Authentication authentication) {
        IncidentReport incident = find(id);
        actionRepository.save(IncidentAction.builder()
                .incident(incident).actionBy(currentUser(authentication)).actionType(request.actionType())
                .targetType(trim(request.targetType())).targetId(request.targetId())
                .oldValue(trim(request.oldValue())).newValue(trim(request.newValue()))
                .note(trim(request.note())).build());
        return toResponse(incident);
    }

    private void validateHierarchy(Event event, Track track, Round round, Team team, Submission submission) {
        UUID expectedEvent = event.getId();
        UUID expectedTrack = null;
        if (track != null) {
            requireSame(expectedEvent, track.getEvent().getId(), "Track does not belong to the selected event");
            expectedTrack = track.getId();
        }
        if (round != null) {
            requireSame(expectedEvent, round.getTrack().getEvent().getId(), "Round does not belong to the selected event");
            expectedTrack = mergeTrack(expectedTrack, round.getTrack().getId(), "Round and track do not match");
        }
        if (team != null) {
            requireSame(expectedEvent, team.getEvent().getId(), "Team does not belong to the selected event");
            if (team.getTrack() != null) {
                expectedTrack = mergeTrack(expectedTrack, team.getTrack().getId(), "Team and track do not match");
            } else if (expectedTrack != null) {
                throw ApiException.badRequest("Unassigned team does not belong to the selected track");
            }
        }
        if (submission != null) {
            requireSame(expectedEvent, submission.getRound().getTrack().getEvent().getId(),
                    "Submission does not belong to the selected event");
            expectedTrack = mergeTrack(expectedTrack, submission.getRound().getTrack().getId(),
                    "Submission round and selected track do not match");
            expectedTrack = mergeTrack(expectedTrack, submission.getTeam().getTrack().getId(),
                    "Submission team and selected track do not match");
            if (round != null) {
                requireSame(round.getId(), submission.getRound().getId(),
                        "Submission does not belong to the selected round");
            }
            if (team != null) {
                requireSame(team.getId(), submission.getTeam().getId(),
                        "Submission does not belong to the selected team");
            }
        }
    }

    private UUID mergeTrack(UUID expected, UUID actual, String message) {
        if (expected != null) {
            requireSame(expected, actual, message);
        }
        return actual;
    }

    private void requireSame(UUID expected, UUID actual, String message) {
        if (!expected.equals(actual)) {
            throw ApiException.badRequest(message);
        }
    }

    private IncidentReport find(UUID id) {
        return reportRepository.findWithRelationsById(id)
                .orElseThrow(() -> ApiException.notFound("Incident not found: " + id));
    }

    private IncidentResponse toResponse(IncidentReport incident) {
        return IncidentMapper.toResponse(
                incident,
                evidenceRepository.findByIncidentIdOrderByCreatedAtAsc(incident.getId()),
                actionRepository.findByIncidentIdOrderByCreatedAtAsc(incident.getId()));
    }
    private void record(IncidentReport i, TimelineEventType type, String title, String description, String suffix) {
        if (timeline == null) return;
        TimelineScope scope = i.getRound() != null || i.getTrack() != null
                ? TimelineScope.STAFF_PRIVATE : TimelineScope.COORDINATOR_PRIVATE;
        timeline.record(new TimelineEventRequest(i.getEvent().getId(), i.getTeam() == null ? null : i.getTeam().getId(),
                i.getRound() == null ? null : i.getRound().getId(), i.getTrack() == null ? null : i.getTrack().getId(),
                type, TimelineSourceType.INCIDENT, i.getId(), scope, title, description, null,
                "incident:" + i.getId() + ":" + suffix));
    }

    private User currentUser(Authentication authentication) {
        CurrentUser current = authorizationService.current(authentication);
        return userRepository.findById(current.getId())
                .orElseThrow(() -> ApiException.notFound("User not found: " + current.getId()));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
