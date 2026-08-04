package vn.edu.fpt.seal.modules.submission.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.enums.EventStatus;
import vn.edu.fpt.seal.common.enums.TeamStatus;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.recognition.service.TeamRecognitionService;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;
import vn.edu.fpt.seal.modules.submission.dto.SubmissionResponse;
import vn.edu.fpt.seal.modules.submission.dto.UpdateSubmissionRequest;
import vn.edu.fpt.seal.modules.submission.dto.UpsertSubmissionRequest;
import vn.edu.fpt.seal.modules.submission.entity.Submission;
import vn.edu.fpt.seal.modules.submission.mapper.SubmissionMapper;
import vn.edu.fpt.seal.modules.submission.repository.SubmissionRepository;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.team.repository.TeamMemberRepository;
import vn.edu.fpt.seal.modules.team.repository.TeamRepository;
import vn.edu.fpt.seal.modules.timeline.TimelineEventType;
import vn.edu.fpt.seal.modules.timeline.TimelineScope;
import vn.edu.fpt.seal.modules.timeline.TimelineSourceType;
import vn.edu.fpt.seal.modules.timeline.dto.TimelineEventRequest;
import vn.edu.fpt.seal.modules.timeline.service.TimelineService;
import vn.edu.fpt.seal.modules.track.repository.TrackRepository;
import vn.edu.fpt.seal.security.AuthorizationService;
import vn.edu.fpt.seal.security.CurrentUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {
    private final SubmissionRepository submissionRepository;
    private final RoundRepository roundRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final AuthorizationService authorizationService;
    private final TrackRepository trackRepository;
    private final TeamRecognitionService recognitionService;
    @Autowired
    private TimelineService timelineService;

    @Transactional(readOnly = true)
    public Page<SubmissionResponse> list(UUID eventId, UUID roundId, UUID teamId, UUID trackId, Pageable pageable, Authentication auth) {
        CurrentUser user = authorizationService.current(auth);
        if (eventId == null && teamId != null) {
            eventId = teamRepository.findWithTrackById(teamId)
                    .orElseThrow(() -> ApiException.notFound("Team not found: " + teamId))
                    .getTrack().getEvent().getId();
        }
        if (eventId == null) throw ApiException.badRequest("eventId is required unless teamId safely determines it");
        UUID scopedEventId = eventId;
        if (trackId != null && !trackRepository.findById(trackId)
                .filter(t -> t.getEvent().getId().equals(scopedEventId)).isPresent()) {
            throw ApiException.badRequest("Track does not belong to the selected event");
        }
        if (teamId != null && !teamRepository.findWithTrackById(teamId)
                .filter(t -> t.getTrack().getEvent().getId().equals(scopedEventId)).isPresent()) {
            throw ApiException.badRequest("Team does not belong to the selected event");
        }
        if (roundId != null) {
            Round round = roundRepository.findById(roundId).orElseThrow(() -> ApiException.notFound("Round not found: " + roundId));
            if (!round.getTrack().getEvent().getId().equals(scopedEventId))
                throw ApiException.badRequest("Round does not belong to the selected event");
            if (trackId != null && !round.getTrack().getId().equals(trackId))
                throw ApiException.badRequest("Round and track do not match");
        }
        Page<Submission> submissions = submissionRepository.searchByEvent(scopedEventId, roundId, trackId, pageable);
        submissions.forEach(submission -> authorizationService.require(
                authorizationService.canReadSubmission(user, submission),
                "You are not authorized to view one or more requested submissions"));
        Map<UUID, List<vn.edu.fpt.seal.modules.recognition.dto.RecognitionDtos.Summary>> recognitionByTeam =
                recognitionService.activeByTeamIds(submissions.getContent().stream()
                        .map(submission -> submission.getTeam().getId()).toList());
        return submissions.map(submission -> SubmissionMapper.toResponse(submission,
                recognitionByTeam.getOrDefault(submission.getTeam().getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public SubmissionResponse get(UUID id, Authentication auth) {
        Submission submission = findOrThrow(id);
        authorizationService.require(
                authorizationService.canReadSubmission(authorizationService.current(auth), submission),
                "You are not authorized to view this submission");
        return toResponse(submission);
    }

    @Transactional
    public SubmissionResponse submit(UpsertSubmissionRequest req, Authentication auth) {
        Round round = roundRepository.findById(req.roundId()).orElseThrow(() -> ApiException.notFound("Round not found: " + req.roundId()));
        Team team = teamRepository.findWithTrackById(req.teamId()).orElseThrow(() -> ApiException.notFound("Team not found: " + req.teamId()));
        validateRoundTeam(round, team);
        ensureCanSubmit(team, auth);
        ensureSubmissionOpen(round);
        Submission existing = submissionRepository.findByRoundIdAndTeamId(round.getId(), team.getId()).orElse(null);
        boolean created = existing == null;
        Submission s = created ? Submission.builder().round(round).team(team).build() : existing;
        String oldReview = s.getReviewStatus();
        apply(s, req.repoUrl(), req.demoUrl(), req.slideUrl(), req.reportUrl(), req.apiMetadata(), req.projectName(), req.version(), req.reviewStatus());
        ensureAtLeastOneUrl(s);
        s = submissionRepository.save(s);
        recordSubmission(s, created ? TimelineEventType.SUBMISSION_CREATED : TimelineEventType.SUBMISSION_UPDATED,
                created ? "Submission created" : "Submission updated",
                created ? "The team created its round submission" : "The team updated its round submission",
                created ? "created" : "update:" + java.util.Objects.hash(req.projectName(), req.version(), req.reviewStatus()));
        if (!java.util.Objects.equals(oldReview, s.getReviewStatus())) recordReview(s);
        log.info("Submission upserted: id={}, round={}, team={}", s.getId(), round.getId(), team.getId());
        return toResponse(s);
    }

    @Transactional
    public SubmissionResponse update(UUID id, UpdateSubmissionRequest req, Authentication auth) {
        Submission s = findOrThrow(id);
        ensureCanSubmit(s.getTeam(), auth);
        ensureSubmissionOpen(s.getRound());
        String oldReview = s.getReviewStatus();
        apply(s, req.repoUrl(), req.demoUrl(), req.slideUrl(), req.reportUrl(), req.apiMetadata(), req.projectName(), req.version(), req.reviewStatus());
        ensureAtLeastOneUrl(s);
        recordSubmission(s, TimelineEventType.SUBMISSION_UPDATED, "Submission updated",
                "The team updated its round submission",
                "update:" + java.util.Objects.hash(req.projectName(), req.version(), req.reviewStatus()));
        if (!java.util.Objects.equals(oldReview, s.getReviewStatus())) recordReview(s);
        return toResponse(s);
    }

    @Transactional
    public void delete(UUID id) {
        Submission s = findOrThrow(id);
        ensureSubmissionOpen(s.getRound());
        submissionRepository.delete(s);
    }

    private Submission findOrThrow(UUID id) {
        return submissionRepository.findWithRelationsById(id).orElseThrow(() -> ApiException.notFound("Submission not found: " + id));
    }

    private SubmissionResponse toResponse(Submission submission) {
        return SubmissionMapper.toResponse(submission,
                recognitionService.activeByTeamIds(List.of(submission.getTeam().getId()))
                        .getOrDefault(submission.getTeam().getId(), List.of()));
    }

    private void apply(Submission s, String repoUrl, String demoUrl, String slideUrl, String reportUrl, String apiMetadata, String projectName, String version, String reviewStatus) {
        if (repoUrl != null) s.setRepoUrl(blankToNull(repoUrl));
        if (demoUrl != null) s.setDemoUrl(blankToNull(demoUrl));
        if (slideUrl != null) s.setSlideUrl(blankToNull(slideUrl));
        if (reportUrl != null) s.setReportUrl(blankToNull(reportUrl));
        if (apiMetadata != null) s.setApiMetadata(blankToNull(apiMetadata));
        if (projectName != null) s.setProjectName(blankToNull(projectName));
        if (version != null) s.setVersion(blankToNull(version));
        if (reviewStatus != null) s.setReviewStatus(blankToNull(reviewStatus));
    }

    private void validateRoundTeam(Round round, Team team) {
        if (!round.getTrack().getId().equals(team.getTrack().getId()))
            throw ApiException.badRequest("Team must belong to the same track as round");
        if (team.getStatus() == TeamStatus.disqualified)
            throw ApiException.badRequest("Disqualified team cannot submit");
        EventStatus status = round.getTrack().getEvent().getStatus();
        if (status == EventStatus.completed || status == EventStatus.cancelled)
            throw ApiException.badRequest("Cannot submit in event status " + status);
    }

    private void ensureSubmissionOpen(Round round) {
        if (LocalDateTime.now().isAfter(round.getSubmissionDeadline()))
            throw ApiException.badRequest("Submission deadline has passed");
    }

    private void ensureCanSubmit(Team team, Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) throw ApiException.forbidden("Authentication required");
        boolean coordinator = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_COORDINATOR"));
        if (coordinator) return;
        if (!(auth.getPrincipal() instanceof CurrentUser currentUser))
            throw ApiException.forbidden("Invalid principal");
        if (!teamMemberRepository.existsByTeamIdAndUserId(team.getId(), currentUser.getId()))
            throw ApiException.forbidden("Only team members can submit for this team");
    }

    private void ensureAtLeastOneUrl(Submission s) {
        if (s.getRepoUrl() == null && s.getDemoUrl() == null && s.getSlideUrl() == null && s.getReportUrl() == null) {
            throw ApiException.badRequest("At least one URL (repoUrl, demoUrl, slideUrl, or reportUrl) is required");
        }
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private void recordSubmission(Submission s, TimelineEventType type, String title, String description, String suffix) {
        if (timelineService == null) return;
        timelineService.record(new TimelineEventRequest(s.getRound().getTrack().getEvent().getId(), s.getTeam().getId(),
                s.getRound().getId(), s.getTeam().getTrack().getId(), type, TimelineSourceType.SUBMISSION,
                s.getId(), TimelineScope.TEAM_PRIVATE, title, description, null,
                "submission:" + s.getId() + ":" + suffix));
    }

    private void recordReview(Submission s) {
        if (timelineService == null) return;
        timelineService.record(new TimelineEventRequest(s.getRound().getTrack().getEvent().getId(), s.getTeam().getId(),
                s.getRound().getId(), s.getTeam().getTrack().getId(), TimelineEventType.SUBMISSION_REVIEW_STATUS_CHANGED,
                TimelineSourceType.SUBMISSION, s.getId(), TimelineScope.STAFF_PRIVATE,
                "Submission review status changed", "A submission review milestone changed", null,
                "submission:" + s.getId() + ":review:" + s.getReviewStatus()));
    }
}
