package vn.edu.fpt.seal.modules.seeding.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.enums.*;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.appeal.repository.AppealRepository;
import vn.edu.fpt.seal.modules.audit.entity.AuditLog;
import vn.edu.fpt.seal.modules.audit.repository.AuditLogRepository;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.event.repository.EventRepository;
import vn.edu.fpt.seal.modules.resultversion.entity.*;
import vn.edu.fpt.seal.modules.resultversion.repository.*;
import vn.edu.fpt.seal.modules.recognition.service.TeamRecognitionService;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;
import vn.edu.fpt.seal.modules.round.service.CompetitionLifecycleService;
import vn.edu.fpt.seal.modules.seeding.dto.SeedingDtos;
import vn.edu.fpt.seal.modules.seeding.entity.*;
import vn.edu.fpt.seal.modules.seeding.repository.*;
import vn.edu.fpt.seal.modules.team.entity.*;
import vn.edu.fpt.seal.modules.team.repository.*;
import vn.edu.fpt.seal.modules.track.repository.TrackRepository;
import vn.edu.fpt.seal.modules.user.entity.User;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.security.CurrentUser;
import vn.edu.fpt.seal.modules.timeline.*;
import vn.edu.fpt.seal.modules.timeline.dto.TimelineEventRequest;
import vn.edu.fpt.seal.modules.timeline.service.TimelineService;

import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class SeedingService {
    public static final String STAGE = "event_setup";
    private static final int MIN_CONTINUITY = 3;
    private static final String FORMULA =
            "Tier 1: best qualifying rank 1-2; Tier 2: best qualifying rank 3-5; continuity must be at least 3.";

    private final EventRepository events;
    private final RoundRepository rounds;
    private final RoundResultVersionRepository versions;
    private final RoundResultVersionEntryRepository versionEntries;
    private final AppealRepository appeals;
    private final TeamRepository teams;
    private final TrackRepository tracks;
    private final TeamMemberRepository members;
    private final EventTeamFinishRepository finishes;
    private final EventSeedAssignmentRepository assignments;
    private final UserRepository users;
    private final AuditLogRepository audits;
    private final CompetitionLifecycleService lifecycle;
    private final TeamRecognitionService recognitionService;
    private final Clock clock;
    @Autowired private TimelineService timeline;

    public SeedingService(EventRepository events, RoundRepository rounds,
                          RoundResultVersionRepository versions,
                          RoundResultVersionEntryRepository versionEntries,
                          AppealRepository appeals, TeamRepository teams,
                          TrackRepository tracks, TeamMemberRepository members,
                          EventTeamFinishRepository finishes,
                          EventSeedAssignmentRepository assignments,
                          UserRepository users, AuditLogRepository audits,
                          CompetitionLifecycleService lifecycle, Clock clock) {
        this.events = events;
        this.rounds = rounds;
        this.versions = versions;
        this.versionEntries = versionEntries;
        this.appeals = appeals;
        this.teams = teams;
        this.tracks = tracks;
        this.members = members;
        this.finishes = finishes;
        this.assignments = assignments;
        this.users = users;
        this.audits = audits;
        this.lifecycle = lifecycle;
        this.recognitionService = null;
        this.clock = clock;
    }

    @Transactional
    public SeedingDtos.FinalizationResponse finalizeResults(UUID eventId, Authentication authentication) {
        Event event = event(eventId);
        if (event.getStatus() == EventStatus.cancelled) {
            throw ApiException.badRequest("Cancelled events cannot create historical finishes");
        }
        if (event.getStatus() == EventStatus.completed) {
            return finalizationResponse(eventId, 0);
        }
        if (event.getStatus() != EventStatus.ongoing) {
            throw ApiException.badRequest("Only an ongoing event can finalize results");
        }

        User actor = actor(authentication);
        LocalDateTime now = LocalDateTime.now(clock);
        List<Round> finalRounds = finalRounds(eventId);
        if (finalRounds.isEmpty()) throw ApiException.conflict("Event has no final competition rounds");
        requireRoundsForActiveTracks(eventId, finalRounds);

        int created = 0;
        for (Round round : finalRounds) {
            lifecycle.refresh(round);
            if (round.getLifecycleState() != RoundLifecycleState.READY_FOR_AWARDS) {
                throw ApiException.conflict("Final round is not READY_FOR_AWARDS: " + round.getId());
            }
            if (round.getAppealDeadline() == null || now.isBefore(round.getAppealDeadline())) {
                throw ApiException.conflict("Final-round appeal window is still open");
            }
            if (appeals.existsByRoundIdAndStatus(round.getId(), "PENDING")) {
                throw ApiException.conflict("Pending appeals block finalization");
            }
            RoundResultVersion version = versions.findByRoundIdAndStatus(round.getId(), "published")
                    .orElseThrow(() -> ApiException.conflict("Final round has no active published result version"));
            if (version.getAppealDeadline() == null || now.isBefore(version.getAppealDeadline())) {
                throw ApiException.conflict("Published result-version appeal window is still open");
            }
            if (!version.getRound().getId().equals(round.getId())) {
                throw ApiException.badRequest("Result version does not belong to the final round");
            }
            List<RoundResultVersionEntry> entries = versionEntries.findByResultVersionId(version.getId());
            if (entries.isEmpty()) throw ApiException.conflict("Published final result version has no entries");

            for (RoundResultVersionEntry entry : entries) {
                Team team = entry.getTeam();
                validateFinishHierarchy(event, round, version, team, entry);
                Optional<EventTeamFinish> existing = finishes.findByTeamId(team.getId());
                if (existing.isPresent()) {
                    if (!existing.get().getResultVersion().getId().equals(version.getId())) {
                        throw ApiException.conflict("Team already has a finish from another result version");
                    }
                    continue;
                }
                finishes.save(EventTeamFinish.builder()
                        .event(event).track(round.getTrack()).team(team).teamProfile(team.getTeamProfile())
                        .finalRound(round).resultVersion(version).finalRank(entry.getRank())
                        .finalScore(entry.getTotalScore())
                        .completionStatus(team.getStatus() == TeamStatus.disqualified
                                ? "disqualified" : "completed")
                        .completedAt(now).createdBy(actor).build());
                created++;
            }
        }
        audits.save(AuditLog.builder().user(actor).action(AuditAction.FINALIZE_RESULTS)
                .targetType("event").targetId(eventId).newValue(Integer.toString(created))
                .details("Created immutable final finishes for all final tracks").build());
        if (timeline != null) {
            for (Round round : finalRounds) {
                timeline.record(new TimelineEventRequest(eventId, null, round.getId(), round.getTrack().getId(),
                        TimelineEventType.FINAL_ROUND_READY_FOR_AWARDS, TimelineSourceType.ROUND,
                        round.getId(), TimelineScope.EVENT_PARTICIPANTS, "Final round ready for awards",
                        "The final round completed its appeal lifecycle", null,
                        "round:" + round.getId() + ":ready-for-awards"));
                timeline.record(new TimelineEventRequest(eventId, null, round.getId(), round.getTrack().getId(),
                        TimelineEventType.TRACK_RESULTS_FINALIZED, TimelineSourceType.EVENT_FINALIZATION,
                        round.getId(), TimelineScope.EVENT_PARTICIPANTS, "Track results finalized",
                        "Immutable final results were created for the track", null,
                        "event:" + eventId + ":track:" + round.getTrack().getId() + ":finalized"));
            }
            timeline.record(new TimelineEventRequest(eventId, null, null, null,
                    TimelineEventType.HISTORICAL_FINISHES_CREATED, TimelineSourceType.EVENT_FINALIZATION,
                    eventId, TimelineScope.EVENT_PARTICIPANTS, "Historical finishes recorded",
                    "Immutable competition finish records were created", null,
                    "event:" + eventId + ":historical-finishes"));
            timeline.record(new TimelineEventRequest(eventId, null, null, null,
                    TimelineEventType.FINAL_RESULTS_FINALIZED, TimelineSourceType.EVENT_FINALIZATION,
                    eventId, TimelineScope.EVENT_PARTICIPANTS, "Final results finalized",
                    "Immutable final results were finalized for the event", null,
                    "event:" + eventId + ":final-results"));
            timeline.record(new TimelineEventRequest(eventId, null, null, null,
                    TimelineEventType.EVENT_READY_FOR_AWARDS, TimelineSourceType.EVENT_FINALIZATION,
                    eventId, TimelineScope.EVENT_PARTICIPANTS, "Event ready for awards",
                    "The event completed finalization and is ready for awards", null,
                    "event:" + eventId + ":ready-for-awards"));
        }
        if (recognitionService != null) {
            recognitionService.evaluateProfiles(
                    finishes.findByEventId(eventId).stream()
                            .map(f -> f.getTeamProfile().getId()).collect(Collectors.toSet()),
                    actor);
        }
        return finalizationResponse(eventId, created);
    }

    @Transactional(readOnly = true)
    public void requireEventFinalized(UUID eventId) {
        List<Round> finalRounds = finalRounds(eventId);
        if (finalRounds.isEmpty()) throw ApiException.conflict("Event has no final rounds");
        requireRoundsForActiveTracks(eventId, finalRounds);
        for (Round round : finalRounds) {
            if (finishes.countByFinalRoundId(round.getId()) == 0) {
                throw ApiException.conflict("Every final track must be snapshotted before event completion");
            }
        }
    }

    @Transactional(readOnly = true)
    public SeedingDtos.CandidateResponse candidates(UUID eventId, UUID trackId) {
        Event event = event(eventId);
        if (event.getStatus() == EventStatus.completed || event.getStatus() == EventStatus.cancelled) {
            throw ApiException.badRequest("Seed candidates are only available before event completion");
        }
        if (trackId != null && tracks.findById(trackId)
                .filter(track -> track.getEvent().getId().equals(eventId)).isEmpty()) {
            throw ApiException.badRequest("Track does not belong to event");
        }
        List<Team> currentTeams = teams.findByTrackEventId(eventId).stream()
                .filter(team -> team.getStatus() == TeamStatus.active)
                .filter(team -> trackId == null || team.getTrack().getId().equals(trackId))
                .toList();
        if (currentTeams.isEmpty()) return new SeedingDtos.CandidateResponse(
                eventId, trackId, reviewSummary(0, List.of()), List.of());

        Set<UUID> profileIds = currentTeams.stream().map(t -> t.getTeamProfile().getId())
                .collect(Collectors.toSet());
        List<EventTeamFinish> history = finishes.findByTeamProfileIdIn(profileIds).stream()
                .filter(f -> f.getEvent().getStatus() == EventStatus.completed)
                .filter(f -> "completed".equals(f.getCompletionStatus()))
                .filter(f -> f.getFinalRank() >= 1 && f.getFinalRank() <= 5)
                .toList();
        Set<UUID> rosterTeamIds = new HashSet<>();
        currentTeams.forEach(t -> rosterTeamIds.add(t.getId()));
        history.forEach(f -> rosterTeamIds.add(f.getTeam().getId()));
        Map<UUID, List<TeamMember>> rosterByTeam = members.findByTeamIdIn(rosterTeamIds).stream()
                .collect(Collectors.groupingBy(tm -> tm.getTeam().getId()));
        Map<UUID, List<EventTeamFinish>> historyByProfile = history.stream()
                .collect(Collectors.groupingBy(f -> f.getTeamProfile().getId()));
        Map<UUID, EventSeedAssignment> decisionByTeam = assignments.findByEventId(eventId).stream()
                .collect(Collectors.toMap(a -> a.getTeam().getId(), Function.identity()));
        Map<UUID, List<vn.edu.fpt.seal.modules.recognition.dto.RecognitionDtos.Summary>>
                recognitionsByTeam = recognitionService == null ? Map.of()
                : recognitionService.activeByTeamIds(
                        currentTeams.stream().map(Team::getId).toList());

        List<SeedingDtos.Candidate> candidates = new ArrayList<>();
        for (Team team : currentTeams) {
            Set<UUID> currentIds = rosterByTeam.getOrDefault(team.getId(), List.of()).stream()
                    .map(tm -> tm.getUser().getId()).collect(Collectors.toSet());
            List<SeedingDtos.ContinuityEvidence> evidence = new ArrayList<>();
            for (EventTeamFinish finish : historyByProfile.getOrDefault(team.getTeamProfile().getId(), List.of())) {
                List<TeamMember> historicalRoster = rosterByTeam.getOrDefault(finish.getTeam().getId(), List.of());
                List<SeedingDtos.RosterMember> matching = historicalRoster.stream()
                        .filter(tm -> currentIds.contains(tm.getUser().getId()))
                        .collect(Collectors.toMap(tm -> tm.getUser().getId(), Function.identity(),
                                (left, right) -> left, LinkedHashMap::new))
                        .values().stream()
                        .map(tm -> new SeedingDtos.RosterMember(tm.getUser().getId(), tm.getUser().getFullName()))
                        .toList();
                evidence.add(new SeedingDtos.ContinuityEvidence(finish.getId(), finish.getEvent().getId(),
                        finish.getEvent().getTitle(), finish.getTrack().getId(), finish.getTrack().getName(),
                        finish.getFinalRank(), finish.getResultVersion().getId(), finish.getCompletedAt(), historicalRoster.size(),
                        matching.size(), matching.size() >= MIN_CONTINUITY, matching));
            }
            List<SeedingDtos.ContinuityEvidence> qualifying = evidence.stream()
                    .filter(SeedingDtos.ContinuityEvidence::qualifiedByContinuity).toList();
            if (qualifying.isEmpty()) continue;
            int bestRank = qualifying.stream().mapToInt(SeedingDtos.ContinuityEvidence::finalRank).min().orElseThrow();
            UUID latestFinishId = evidence.stream()
                    .max(Comparator.comparing(SeedingDtos.ContinuityEvidence::completedAt))
                    .map(SeedingDtos.ContinuityEvidence::historicalFinishId).orElseThrow();
            String tier = bestRank <= 2 ? "TIER_1" : "TIER_2";
            List<SeedingDtos.RosterMember> currentRoster = rosterByTeam.getOrDefault(team.getId(), List.of()).stream()
                    .map(tm -> new SeedingDtos.RosterMember(tm.getUser().getId(), tm.getUser().getFullName())).toList();
            candidates.add(new SeedingDtos.Candidate(team.getId(), team.getName(), team.getTeamProfile().getId(),
                    team.getTeamProfile().getCanonicalName(), team.getTrack().getId(), team.getTrack().getName(),
                    currentRoster, evidence.stream()
                    .sorted(Comparator.comparingInt(SeedingDtos.ContinuityEvidence::finalRank)).toList(),
                    bestRank, latestFinishId, evidence.size(), tier, FORMULA,
                    map(decisionByTeam.get(team.getId())),
                    recognitionsByTeam.getOrDefault(team.getId(), List.of()),
                    "Top-five immutable finish with at least three returning historical roster members"));
        }
        candidates.sort(Comparator.comparingInt(SeedingDtos.Candidate::bestHistoricalRank)
                .thenComparing(SeedingDtos.Candidate::teamName));
        List<EventSeedAssignment> candidateDecisions = candidates.stream()
                .map(c -> decisionByTeam.get(c.teamId())).filter(Objects::nonNull).toList();
        return new SeedingDtos.CandidateResponse(eventId, trackId,
                reviewSummary(candidates.size(), candidateDecisions), candidates);
    }

    @Transactional(readOnly = true)
    public List<SeedingDtos.Assignment> seeds(UUID eventId) {
        event(eventId);
        return assignments.findByEventId(eventId).stream().map(this::map).toList();
    }

    @Transactional
    public SeedingDtos.Assignment decide(UUID eventId, UUID teamId,
                                         SeedingDtos.SeedDecisionRequest request,
                                         Authentication authentication) {
        Event event = event(eventId);
        Team team = teams.findWithTrackById(teamId)
                .orElseThrow(() -> ApiException.notFound("Team not found: " + teamId));
        if (!team.getTrack().getEvent().getId().equals(eventId)) {
            throw ApiException.badRequest("Team does not belong to event");
        }
        SeedingDtos.Candidate candidate = candidates(eventId, team.getTrack().getId()).candidates().stream()
                .filter(c -> c.teamId().equals(teamId)).findFirst()
                .orElseThrow(() -> ApiException.badRequest("Team is not an eligible seed candidate"));
        if ("overridden".equals(request.status())
                && (request.rationale() == null || request.rationale().isBlank())) {
            throw ApiException.badRequest("Override requires a rationale");
        }
        SeedingDtos.ContinuityEvidence source = selectSource(candidate, request.candidateSourceFinishId());
        EventTeamFinish sourceFinish = finishes.findById(source.historicalFinishId()).orElseThrow();
        EventSeedAssignment assignment = assignments
                .findByEventIdAndTeamIdAndCompetitionStage(eventId, teamId, STAGE)
                .orElseGet(EventSeedAssignment::new);
        User actor = actor(authentication);
        assignment.setEvent(event);
        assignment.setTrack(team.getTrack());
        assignment.setTeam(team);
        assignment.setTeamProfile(team.getTeamProfile());
        assignment.setCompetitionStage(STAGE);
        assignment.setStatus(request.status());
        assignment.setCandidateSourceFinish(sourceFinish);
        assignment.setContinuityCount(source.returningMemberCount());
        assignment.setAssignedBy(actor);
        assignment.setRationale(trim(request.rationale()));
        if ("rejected".equals(request.status())) {
            assignment.setSeedNumber(null);
            assignment.setSeedTier(null);
        } else {
            assignment.setSeedNumber(request.seedNumber());
            assignment.setSeedTier(request.seedTier() == null || request.seedTier().isBlank()
                    ? candidate.suggestedSeedTier() : request.seedTier().trim().toUpperCase(Locale.ROOT));
        }
        try {
            assignment = assignments.saveAndFlush(assignment);
        } catch (DataIntegrityViolationException ex) {
            if (!isSeedUniquenessConflict(ex)) throw ex;
            throw ApiException.conflict("Seed number or team assignment conflicts within this event track and stage");
        }
        audits.save(AuditLog.builder().user(actor).team(team).action(AuditAction.ASSIGN_SEED)
                .targetType("event_seed_assignment").targetId(assignment.getId())
                .newValue(assignment.getStatus()).details("EC seed decision for event " + eventId).build());
        if (timeline != null) {
            TimelineEventType type = "rejected".equals(assignment.getStatus()) ? TimelineEventType.SEED_REMOVED
                    : "overridden".equals(assignment.getStatus()) ? TimelineEventType.SEED_OVERRIDDEN
                    : TimelineEventType.SEED_CONFIRMED;
            TimelineScope scope = "rejected".equals(assignment.getStatus())
                    ? TimelineScope.COORDINATOR_PRIVATE : TimelineScope.EVENT_PARTICIPANTS;
            timeline.record(new TimelineEventRequest(eventId, scope == TimelineScope.EVENT_PARTICIPANTS ? teamId : null,
                    null, team.getTrack().getId(), type, TimelineSourceType.SEED_ASSIGNMENT,
                    assignment.getId(), scope, type == TimelineEventType.SEED_OVERRIDDEN ? "Seed assignment overridden" : "Seed assignment confirmed",
                    "rejected".equals(assignment.getStatus())
                            ? "A coordinator removed a seed assignment"
                            : "A coordinator completed a seed assignment decision", null,
                    "seed:" + assignment.getId() + ":status:" + assignment.getStatus()));
        }
        return map(assignment);
    }

    @Transactional
    public void remove(UUID eventId, UUID teamId, Authentication authentication) {
        EventSeedAssignment assignment = assignments
                .findByEventIdAndTeamIdAndCompetitionStage(eventId, teamId, STAGE)
                .orElseThrow(() -> ApiException.notFound("Seed assignment not found"));
        User actor = actor(authentication);
        UUID assignmentId = assignment.getId();
        assignments.delete(assignment);
        audits.save(AuditLog.builder().user(actor).team(assignment.getTeam()).action(AuditAction.REMOVE_SEED)
                .targetType("event_seed_assignment").targetId(assignmentId)
                .oldValue(assignment.getStatus()).details("Removed event seed metadata").build());
        if (timeline != null) timeline.record(new TimelineEventRequest(eventId, null, null,
                assignment.getTrack().getId(), TimelineEventType.SEED_REMOVED, TimelineSourceType.SEED_ASSIGNMENT,
                assignmentId, TimelineScope.COORDINATOR_PRIVATE, "Seed assignment removed",
                "A seed assignment was removed", null, "seed:" + assignmentId + ":removed"));
    }

    private boolean isSeedUniquenessConflict(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && (message.contains("uq_event_seed_team_stage")
                    || message.contains("uq_event_seed_number_active"))) return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public SeedingDtos.ReviewSummary setupReview(UUID eventId) {
        return candidates(eventId, null).summary();
    }

    @Transactional(readOnly = true)
    public List<EventSeedAssignment> confirmedAssignments(UUID eventId) {
        return assignments.findByEventId(eventId).stream()
                .filter(a -> Set.of("confirmed", "overridden").contains(a.getStatus())).toList();
    }

    private void validateFinishHierarchy(Event event, Round round, RoundResultVersion version,
                                         Team team, RoundResultVersionEntry entry) {
        if (entry.getRank() == null || entry.getRank() <= 0) {
            throw ApiException.badRequest("Final result entry rank must be positive");
        }
        if (!team.getTrack().getEvent().getId().equals(event.getId())
                || !team.getTrack().getId().equals(round.getTrack().getId())
                || team.getTeamProfile() == null
                || !entry.getResultVersion().getId().equals(version.getId())) {
            throw ApiException.badRequest("Final result hierarchy mismatch");
        }
        if (!"published".equals(version.getStatus())) {
            throw ApiException.conflict("Superseded result versions cannot be finalized");
        }
    }

    private List<Round> finalRounds(UUID eventId) {
        return rounds.findByTrackEventId(eventId, Pageable.unpaged()).stream()
                .collect(Collectors.groupingBy(r -> r.getTrack().getId()))
                .values().stream()
                .map(list -> list.stream().max(Comparator.comparingInt(Round::getSequenceNumber)).orElseThrow())
                .toList();
    }

    private void requireRoundsForActiveTracks(UUID eventId, List<Round> finalRounds) {
        Set<UUID> finalizedTrackIds = finalRounds.stream().map(round -> round.getTrack().getId())
                .collect(Collectors.toSet());
        Set<UUID> activeTrackIds = teams.findByTrackEventId(eventId).stream()
                .filter(team -> team.getStatus() == TeamStatus.active)
                .map(team -> team.getTrack().getId()).collect(Collectors.toSet());
        if (!finalizedTrackIds.containsAll(activeTrackIds)) {
            throw ApiException.conflict("Every active competition track requires a final round");
        }
    }

    private SeedingDtos.FinalizationResponse finalizationResponse(UUID eventId, int created) {
        List<SeedingDtos.FinalizedFinish> mapped = finishes.findByEventId(eventId).stream()
                .map(f -> new SeedingDtos.FinalizedFinish(f.getId(), f.getTeam().getId(),
                        f.getTeamProfile().getId(), f.getTrack().getId(), f.getFinalRound().getId(),
                        f.getResultVersion().getId(), f.getFinalRank(), f.getFinalScore(),
                        f.getCompletionStatus())).toList();
        return new SeedingDtos.FinalizationResponse(eventId, created, mapped.size() - created, mapped);
    }

    private SeedingDtos.ReviewSummary reviewSummary(int eligible, List<EventSeedAssignment> decisions) {
        int confirmed = (int) decisions.stream().filter(a -> "confirmed".equals(a.getStatus())).count();
        int rejected = (int) decisions.stream().filter(a -> "rejected".equals(a.getStatus())).count();
        int overridden = (int) decisions.stream().filter(a -> "overridden".equals(a.getStatus())).count();
        int unreviewed = Math.max(0, eligible - decisions.size());
        String warning = unreviewed == 0 ? null
                : unreviewed + " eligible seed candidate(s) remain unreviewed. Setup will continue without claiming seed separation.";
        return new SeedingDtos.ReviewSummary(eligible, confirmed, rejected, overridden, unreviewed, warning);
    }

    private SeedingDtos.ContinuityEvidence selectSource(SeedingDtos.Candidate candidate, UUID requestedId) {
        return candidate.supportingFinishes().stream()
                .filter(SeedingDtos.ContinuityEvidence::qualifiedByContinuity)
                .filter(e -> requestedId == null || e.historicalFinishId().equals(requestedId))
                .min(Comparator.comparingInt(SeedingDtos.ContinuityEvidence::finalRank))
                .orElseThrow(() -> ApiException.badRequest("Selected source finish does not qualify"));
    }

    private SeedingDtos.Assignment map(EventSeedAssignment assignment) {
        if (assignment == null) return null;
        return new SeedingDtos.Assignment(assignment.getId(), assignment.getTeam().getId(),
                assignment.getTrack().getId(), assignment.getSeedNumber(), assignment.getSeedTier(),
                assignment.getCandidateSourceFinish() == null ? null : assignment.getCandidateSourceFinish().getId(),
                assignment.getContinuityCount(), assignment.getStatus(), assignment.getRationale(),
                assignment.getAssignedAt());
    }

    private Event event(UUID eventId) {
        return events.findById(eventId)
                .orElseThrow(() -> ApiException.notFound("Event not found: " + eventId));
    }

    private User actor(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser current)) {
            throw ApiException.unauthorized("Authentication required");
        }
        return users.findById(current.getId()).orElseThrow(() -> ApiException.notFound("User not found"));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
