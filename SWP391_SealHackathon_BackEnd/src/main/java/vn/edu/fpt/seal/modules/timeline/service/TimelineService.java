package vn.edu.fpt.seal.modules.timeline.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.enums.EventStatus;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.event.repository.EventRepository;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.team.repository.*;
import vn.edu.fpt.seal.modules.timeline.*;
import vn.edu.fpt.seal.modules.timeline.dto.*;
import vn.edu.fpt.seal.modules.timeline.dto.TimelineDtos.Response;
import vn.edu.fpt.seal.modules.timeline.entity.TimelineEvent;
import vn.edu.fpt.seal.modules.timeline.repository.TimelineEventRepository;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.track.repository.TrackRepository;
import vn.edu.fpt.seal.security.*;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class TimelineService {
    private static final Set<TimelineEventType> COORDINATOR_ONLY = EnumSet.of(
            TimelineEventType.RESULT_RECALCULATION_REQUIRED, TimelineEventType.RECOGNITION_REVOKED);
    private static final Set<TimelineEventType> STAFF_ONLY = EnumSet.of(
            TimelineEventType.INCIDENT_SUBMITTED, TimelineEventType.INCIDENT_UNDER_REVIEW,
            TimelineEventType.INCIDENT_RESOLVED, TimelineEventType.INCIDENT_DISMISSED,
            TimelineEventType.SUBMISSION_REVIEW_STATUS_CHANGED);

    private final TimelineEventRepository repository;
    private final EventRepository events;
    private final TeamMemberRepository members;
    private final TeamRepository teams;
    private final RoundRepository rounds;
    private final TrackRepository tracks;
    private final AuthorizationService authorization;
    private final Clock clock;

    @Transactional
    public TimelineEvent record(TimelineEventRequest request) {
        validate(request);
        Optional<TimelineEvent> existing = repository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) return existing.get();
        Event event = events.findById(request.eventId()).orElseThrow(() -> ApiException.notFound("Event not found"));
        Team team = request.teamId() == null ? null : teams.findById(request.teamId())
                .orElseThrow(() -> ApiException.notFound("Team not found"));
        Round round = request.roundId() == null ? null : rounds.findById(request.roundId())
                .orElseThrow(() -> ApiException.notFound("Round not found"));
        Track track = request.trackId() == null ? null : tracks.findById(request.trackId())
                .orElseThrow(() -> ApiException.notFound("Track not found"));
        validateHierarchy(event, team, round, track);
        TimelineEvent item = TimelineEvent.builder().event(event).team(team).round(round).track(track)
                .type(request.eventType().name()).eventType(request.eventType()).visibilityScope(request.visibility())
                .title(safe(request.title(), 255)).description(safe(request.description(), 2000))
                .metadata(safe(request.metadata(), 2000)).sourceType(request.sourceType())
                .sourceId(request.sourceId()).idempotencyKey(safe(request.idempotencyKey(), 255))
                .occurredAt(LocalDateTime.now(clock)).build();
        try {
            return repository.saveAndFlush(item);
        } catch (DataIntegrityViolationException failure) {
            if (!isIdempotencyConflict(failure)) throw failure;
            throw ApiException.conflict("Timeline event was already recorded");
        }
    }

    /** Compatibility adapter for Batch 9 hooks created before the typed request API. */
    @Transactional
    public TimelineEvent record(Event event, UUID teamId, UUID roundId, UUID trackId, String eventType,
                                TimelineScope scope, String title, String description, String sourceType,
                                UUID sourceId, String key) {
        return record(new TimelineEventRequest(event.getId(), teamId, roundId, trackId,
                TimelineEventType.valueOf(eventType), TimelineSourceType.valueOf(sourceType),
                sourceId, scope, title, description, null, key));
    }

    @Transactional(readOnly = true)
    public Page<Response> event(UUID eventId, UUID roundId, UUID trackId, TimelineEventType eventType,
                                TimelineScope requestedScope, Pageable pageable, Authentication auth) {
        Event event = events.findById(eventId).orElseThrow(() -> ApiException.notFound("Event not found"));
        validateFilterHierarchy(eventId, roundId, trackId);
        CurrentUser user = currentOrNull(auth);
        if (requestedScope != null && (user == null || !authorization.isCoordinator(user)))
            throw ApiException.forbidden("Only coordinators may request a visibility scope");
        int pageNumber = pageable.isPaged() ? pageable.getPageNumber() : 0;
        int pageSize = pageable.isPaged() ? Math.min(pageable.getPageSize(), 100) : 100;
        Pageable ordered = PageRequest.of(pageNumber, pageSize,
                Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id")));
        TimelineScope repositoryScope = user == null ? TimelineScope.EVENT_PUBLIC : requestedScope;
        Page<TimelineEvent> source = repository.search(eventId, roundId, trackId, eventType, repositoryScope, ordered);
        List<Response> visible = source.getContent().stream()
                .filter(i -> canSee(i, event, user)).map(this::toResponse).toList();
        long total = user != null && authorization.isCoordinator(user) ? source.getTotalElements() : visible.size();
        return new PageImpl<>(visible, ordered, total);
    }

    @Transactional(readOnly = true)
    public Page<Response> team(UUID teamId, Pageable pageable, Authentication auth) {
        CurrentUser user = authorization.current(auth);
        if (!authorization.isCoordinator(user) && !authorization.isTeamMember(user, teamId))
            throw ApiException.notFound("Team timeline not found");
        int pageNumber = pageable.isPaged() ? pageable.getPageNumber() : 0;
        int pageSize = pageable.isPaged() ? Math.min(pageable.getPageSize(), 100) : 100;
        Pageable ordered = PageRequest.of(pageNumber, pageSize,
                Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id")));
        return repository.findByTeamId(teamId, ordered).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Response get(UUID id, Authentication auth) {
        TimelineEvent item = repository.findWithRelationsById(id)
                .orElseThrow(() -> ApiException.notFound("Timeline event not found"));
        CurrentUser user = currentOrNull(auth);
        if (!canSee(item, item.getEvent(), user)) throw ApiException.notFound("Timeline event not found");
        return toResponse(item);
    }

    private boolean canSee(TimelineEvent item, Event event, CurrentUser user) {
        if (user != null && authorization.isCoordinator(user)) return true;
        if (item.getVisibilityScope() == TimelineScope.EVENT_PUBLIC)
            return Set.of(EventStatus.published, EventStatus.ongoing, EventStatus.completed).contains(event.getStatus());
        if (user == null) return false;
        return switch (item.getVisibilityScope()) {
            case EVENT_PARTICIPANTS -> members.existsActiveRegistrationInEvent(user.getId(), event.getId())
                    || assignedStaff(item, user);
            case TEAM_PRIVATE -> item.getTeam() != null && authorization.isTeamMember(user, item.getTeam().getId());
            case STAFF_PRIVATE -> assignedStaff(item, user);
            case COORDINATOR_PRIVATE, EVENT_PUBLIC -> false;
        };
    }

    private boolean assignedStaff(TimelineEvent item, CurrentUser user) {
        return (item.getRound() != null && authorization.isAssignedJudge(user, item.getRound().getId()))
                || (item.getTrack() != null && authorization.isAssignedMentor(user, item.getTrack().getId()));
    }

    private void validate(TimelineEventRequest r) {
        if (r == null || r.eventId() == null || r.eventType() == null || r.sourceType() == null
                || r.sourceId() == null || r.visibility() == null || r.idempotencyKey() == null
                || r.idempotencyKey().isBlank() || r.title() == null || r.title().isBlank())
            throw ApiException.badRequest("Incomplete timeline event request");
        if (COORDINATOR_ONLY.contains(r.eventType()) && r.visibility() != TimelineScope.COORDINATOR_PRIVATE)
            throw ApiException.badRequest("This event type must be coordinator-private");
        if (STAFF_ONLY.contains(r.eventType()) && r.visibility() != TimelineScope.STAFF_PRIVATE
                && r.visibility() != TimelineScope.COORDINATOR_PRIVATE)
            throw ApiException.badRequest("This event type must be staff-private");
        if (r.visibility() == TimelineScope.TEAM_PRIVATE && r.teamId() == null)
            throw ApiException.badRequest("TEAM_PRIVATE requires teamId");
        if (r.visibility() == TimelineScope.STAFF_PRIVATE && r.roundId() == null && r.trackId() == null)
            throw ApiException.badRequest("STAFF_PRIVATE requires roundId or trackId");
    }

    private boolean isIdempotencyConflict(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT)
                    .contains("uq_team_timeline_events_idempotency")) {
                return true;
            }
        }
        return false;
    }

    private void validateHierarchy(Event event, Team team, Round round, Track track) {
        if (track != null && !track.getEvent().getId().equals(event.getId())) hierarchy();
        if (round != null && !round.getTrack().getEvent().getId().equals(event.getId())) hierarchy();
        if (team != null && !team.getEvent().getId().equals(event.getId())) hierarchy();
        if (round != null && track != null && !round.getTrack().getId().equals(track.getId())) hierarchy();
        if (team != null && track != null
                && (team.getTrack() == null || !team.getTrack().getId().equals(track.getId()))) hierarchy();
    }

    private void validateFilterHierarchy(UUID eventId, UUID roundId, UUID trackId) {
        Track track = trackId == null ? null : tracks.findById(trackId).orElseThrow(() -> ApiException.notFound("Track not found"));
        Round round = roundId == null ? null : rounds.findById(roundId).orElseThrow(() -> ApiException.notFound("Round not found"));
        if ((track != null && !track.getEvent().getId().equals(eventId))
                || (round != null && !round.getTrack().getEvent().getId().equals(eventId))
                || (track != null && round != null && !round.getTrack().getId().equals(trackId))) hierarchy();
    }

    private void hierarchy() { throw ApiException.badRequest("Timeline filter hierarchy does not match the event"); }
    private CurrentUser currentOrNull(Authentication auth) {
        return auth != null && auth.getPrincipal() instanceof CurrentUser user ? user : null;
    }
    private Response toResponse(TimelineEvent i) {
        return new Response(i.getId(), i.getEvent().getId(), i.getTeam() == null ? null : i.getTeam().getId(),
                i.getRound() == null ? null : i.getRound().getId(), i.getTrack() == null ? null : i.getTrack().getId(),
                i.getEventType(), i.getVisibilityScope(), i.getTitle(), i.getDescription(),
                i.getStatusSnapshot(), i.getOccurredAt());
    }
    private static String safe(String value, int max) {
        if (value == null) return null;
        String clean = value.replaceAll("(?i)(password|token|secret|invite[_ -]?code)\\s*[:=]\\s*\\S+", "[redacted]")
                .replaceAll("\\s+", " ").trim();
        return clean.length() > max ? clean.substring(0, max) : clean;
    }
}
