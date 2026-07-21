package vn.edu.fpt.seal.modules.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.enums.AuditAction;
import vn.edu.fpt.seal.common.enums.EventStatus;
import vn.edu.fpt.seal.common.enums.TeamStatus;
import vn.edu.fpt.seal.common.enums.RoundParticipantStatus;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.audit.entity.AuditLog;
import vn.edu.fpt.seal.modules.audit.repository.AuditLogRepository;
import vn.edu.fpt.seal.modules.event.dto.CreateEventRequest;
import vn.edu.fpt.seal.modules.event.dto.EventResponse;
import vn.edu.fpt.seal.modules.event.dto.SetupCompetitionRequest;
import vn.edu.fpt.seal.modules.event.dto.SetupCompetitionResponse;
import vn.edu.fpt.seal.modules.event.dto.UpdateEventRequest;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.event.mapper.EventMapper;
import vn.edu.fpt.seal.modules.event.repository.EventRepository;
import vn.edu.fpt.seal.modules.participant.entity.RoundParticipant;
import vn.edu.fpt.seal.modules.participant.repository.RoundParticipantRepository;
import vn.edu.fpt.seal.modules.recognition.service.TeamRecognitionService;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.round.entity.RoundDefinition;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;
import vn.edu.fpt.seal.modules.round.repository.RoundDefinitionRepository;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.team.repository.TeamMemberRepository;
import vn.edu.fpt.seal.modules.team.repository.TeamRepository;
import vn.edu.fpt.seal.modules.seeding.entity.EventSeedAssignment;
import vn.edu.fpt.seal.modules.seeding.service.SeedingService;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.track.repository.TrackRepository;
import vn.edu.fpt.seal.modules.user.entity.User;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.security.CurrentUser;
import org.springframework.security.core.Authentication;
import vn.edu.fpt.seal.modules.timeline.TimelineScope;
import vn.edu.fpt.seal.modules.timeline.service.TimelineService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class EventService {

    private final EventRepository eventRepository;
    private final TrackRepository trackRepository;
    private final RoundRepository roundRepository;
    private final TeamRepository teamRepository;
    private final RoundParticipantRepository roundParticipantRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final vn.edu.fpt.seal.modules.round.service.CompetitionLifecycleService lifecycleService;
    private final SeedingService seedingService;
    private final TeamRecognitionService recognitionService;
    private final TimelineService timelineService;
    private final RoundDefinitionRepository roundDefinitionRepository;

    public EventService(EventRepository events, TrackRepository tracks,
                        RoundRepository rounds, TeamRepository teams,
                        RoundParticipantRepository participants,
                        TeamMemberRepository members, AuditLogRepository audits,
                        UserRepository users,
                        vn.edu.fpt.seal.modules.round.service.CompetitionLifecycleService lifecycle,
                        SeedingService seeding) {
        this(events, tracks, rounds, teams, participants, members, audits, users, lifecycle, seeding, null, null, null);
    }

    public EventService(EventRepository events, TrackRepository tracks,
                        RoundRepository rounds, TeamRepository teams,
                        RoundParticipantRepository participants,
                        TeamMemberRepository members, AuditLogRepository audits,
                        UserRepository users,
                        vn.edu.fpt.seal.modules.round.service.CompetitionLifecycleService lifecycle,
                        SeedingService seeding, TeamRecognitionService recognition) {
        this(events, tracks, rounds, teams, participants, members, audits, users, lifecycle, seeding, recognition, null, null);
    }

    @Autowired
    public EventService(EventRepository events, TrackRepository tracks,
                        RoundRepository rounds, TeamRepository teams,
                        RoundParticipantRepository participants,
                        TeamMemberRepository members, AuditLogRepository audits,
                        UserRepository users,
                        vn.edu.fpt.seal.modules.round.service.CompetitionLifecycleService lifecycle,
                        SeedingService seeding, TeamRecognitionService recognition,
                        TimelineService timeline, RoundDefinitionRepository roundDefinitionRepository) {
        this.eventRepository = events;
        this.trackRepository = tracks;
        this.roundRepository = rounds;
        this.teamRepository = teams;
        this.roundParticipantRepository = participants;
        this.teamMemberRepository = members;
        this.auditLogRepository = audits;
        this.userRepository = users;
        this.lifecycleService = lifecycle;
        this.seedingService = seeding;
        this.recognitionService = recognition;
        this.timelineService = timeline;
        this.roundDefinitionRepository = roundDefinitionRepository;
    }

    /** Default track name auto-created while registration is open. */
    private static final String GENERAL_TRACK = "General";

    /** A team needs at least this many members to compete; smaller teams are eliminated at registration close. */
    private static final int MIN_TEAM_SIZE = 3;

    // Status transition rules:
    //   draft     -> published, cancelled
    //   published -> ongoing, cancelled
    //   ongoing   -> completed, cancelled
    //   completed -> (terminal)
    //   cancelled -> (terminal)
    private static final Map<EventStatus, Set<EventStatus>> ALLOWED_TRANSITIONS = Map.of(
            EventStatus.draft, EnumSet.of(EventStatus.published, EventStatus.cancelled),
            EventStatus.published, EnumSet.of(EventStatus.ongoing, EventStatus.cancelled),
            EventStatus.ongoing, EnumSet.of(EventStatus.completed, EventStatus.cancelled),
            EventStatus.completed, EnumSet.noneOf(EventStatus.class),
            EventStatus.cancelled, EnumSet.noneOf(EventStatus.class)
    );

    @Transactional(readOnly = true)
    public Page<EventResponse> list(EventStatus status, Pageable pageable) {
        Page<Event> page = (status == null)
                ? eventRepository.findAll(pageable)
                : eventRepository.findByStatus(status, pageable);
        return page.map(this::toResponseWithCounts);
    }

    @Transactional(readOnly = true)
    public EventResponse get(UUID id) {
        return toResponseWithCounts(findOrThrow(id));
    }

    private EventResponse toResponseWithCounts(Event e) {
        UUID eventId = e.getId();
        long tracks = trackRepository.countByEventId(eventId);
        long rounds = roundRepository.countByTrackEventId(eventId);
        long participants = teamRepository.countByTrackEventId(eventId);
        return EventMapper.toResponse(e, (int) tracks, (int) rounds, participants);
    }

    @Transactional
    public EventResponse create(CreateEventRequest req) {
        String title = req.title().trim();
        if (eventRepository.existsByTitleIgnoreCase(title)) {
            throw ApiException.conflict("Event title already exists");
        }
        Event e = Event.builder()
                .title(title)
                .description(req.description())
                .status(EventStatus.draft)
                .term(req.term())
                .prizePool(req.prizePool())
                .registrationStart(req.registrationStart())
                .registrationEnd(req.registrationEnd())
                .eventStart(req.eventStart())
                .eventEnd(req.eventEnd())
                .build();
        e = eventRepository.save(e);
        log.info("Event created: id={}, title={}", e.getId(), e.getTitle());
        return toResponseWithCounts(e);
    }

    @Transactional
    public EventResponse update(UUID id, UpdateEventRequest req) {
        Event e = findOrThrow(id);
        if (e.getStatus() == EventStatus.completed || e.getStatus() == EventStatus.cancelled) {
            throw ApiException.badRequest("Cannot edit event in status " + e.getStatus());
        }
        if (req.title() != null) {
            String title = req.title().trim();
            if (!title.equalsIgnoreCase(e.getTitle())
                    && eventRepository.existsByTitleIgnoreCase(title)) {
                throw ApiException.conflict("Event title already exists");
            }
            e.setTitle(title);
        }
        if (req.description() != null) {
            e.setDescription(req.description());
        }
        if (req.term() != null) {
            e.setTerm(req.term());
        }
        if (req.prizePool() != null) {
            e.setPrizePool(req.prizePool());
        }
        if (req.registrationStart() != null) {
            e.setRegistrationStart(req.registrationStart());
        }
        if (req.registrationEnd() != null) {
            e.setRegistrationEnd(req.registrationEnd());
        }
        if (req.eventStart() != null) {
            e.setEventStart(req.eventStart());
        }
        if (req.eventEnd() != null) {
            e.setEventEnd(req.eventEnd());
        }
        return toResponseWithCounts(e);
    }

    @Transactional
    public EventResponse changeStatus(UUID id, EventStatus target) {
        return changeStatus(id, target, null);
    }

    @Transactional
    public EventResponse changeStatus(UUID id, EventStatus target, Authentication authentication) {
        Event e = findOrThrow(id);
        EventStatus current = e.getStatus();
        if (current == target) {
            return EventMapper.toResponse(e);
        }
        Set<EventStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, EnumSet.noneOf(EventStatus.class));
        if (!allowed.contains(target)) {
            throw ApiException.badRequest(
                    "Invalid status transition: " + current + " -> " + target);
        }
        if (target == EventStatus.completed) {
            lifecycleService.requireAwardsAllowed(e.getId());
            seedingService.requireEventFinalized(e.getId());
        }
        e.setStatus(target);
        if (timelineService != null && target != EventStatus.completed) {
            vn.edu.fpt.seal.modules.timeline.TimelineEventType timelineType = switch (target) {
                case published -> vn.edu.fpt.seal.modules.timeline.TimelineEventType.EVENT_PUBLISHED;
                case ongoing -> vn.edu.fpt.seal.modules.timeline.TimelineEventType.EVENT_STARTED;
                case completed -> vn.edu.fpt.seal.modules.timeline.TimelineEventType.EVENT_COMPLETED;
                case cancelled -> vn.edu.fpt.seal.modules.timeline.TimelineEventType.EVENT_CANCELLED;
                case draft -> throw ApiException.badRequest("Draft is not a timeline transition");
            };
            timelineService.record(e, null, null, null, timelineType.name(),
                    TimelineScope.EVENT_PUBLIC,
                    "Event " + target.name().toLowerCase(), "Event status changed from " + current + " to " + target,
                    "EVENT_STATUS", e.getId(), "event:" + e.getId() + ":status:" + target);
        }
        if (target == EventStatus.completed && recognitionService != null) {
            User actor = null;
            if (authentication != null && authentication.getPrincipal() instanceof CurrentUser principal) {
                actor = userRepository.findById(principal.getId()).orElse(null);
            }
            recognitionService.evaluateProfiles(
                    teamRepository.findByTrackEventId(e.getId()).stream()
                            .map(Team::getTeamProfile).filter(java.util.Objects::nonNull)
                            .map(profile -> profile.getId()).collect(java.util.stream.Collectors.toSet()),
                    actor);
        }
        if (target == EventStatus.completed && timelineService != null) {
            timelineService.record(e, null, null, null,
                    vn.edu.fpt.seal.modules.timeline.TimelineEventType.EVENT_COMPLETED.name(),
                    TimelineScope.EVENT_PUBLIC, "Event completed",
                    "The event was completed after final results and recognition processing",
                    "EVENT_STATUS", e.getId(), "event:" + e.getId() + ":status:completed");
        }
        log.info("Event {} status: {} -> {}", e.getId(), current, target);
        return EventMapper.toResponse(e);
    }

    @Transactional
    public void delete(UUID id) {
        Event e = findOrThrow(id);
        if (e.getStatus() != EventStatus.draft) {
            throw ApiException.badRequest("Only draft events can be deleted; cancel instead.");
        }
        eventRepository.delete(e);
        log.info("Event deleted: id={}", id);
    }

    /**
     * Open registration: draft -> published. Ensures a "General" track exists so
     * teams can register into the event before the organiser has designed the
     * thematic tracks.
     */
    @Transactional
    public EventResponse openRegistration(UUID id) {
        Event e = findOrThrow(id);
        if (e.getStatus() != EventStatus.draft) {
            throw ApiException.badRequest("Registration can only be opened from draft (current: " + e.getStatus() + ")");
        }
        e.setStatus(EventStatus.published);
        ensureGeneralTrack(e);
        if (timelineService != null) timelineService.record(e, null, null, null, "EVENT_PUBLISHED",
                TimelineScope.EVENT_PUBLIC, "Registration opened", "Event registration is now open",
                "EVENT_STATUS", e.getId(), "event:" + e.getId() + ":published");
        log.info("Event {} registration opened (draft -> published)", e.getId());
        return toResponseWithCounts(e);
    }

    /**
     * Close registration: published -> ongoing. After this, teams can no longer
     * join (enforced in TeamService), and the organiser can build the competition.
     */
    @Transactional
    public EventResponse closeRegistration(UUID id, Authentication auth) {
        Event e = findOrThrow(id);
        if (e.getStatus() != EventStatus.published) {
            throw ApiException.badRequest("Registration can only be closed when published (current: " + e.getStatus() + ")");
        }
        // Teams that never reached the minimum size are eliminated when the form
        // closes: an under-strength team cannot compete. This leaves an audit trail.
        List<Team> teams = teamRepository.findByTrackEventId(e.getId());
        int eliminated = 0;
        for (Team t : teams) {
            if (t.getStatus() != TeamStatus.active) continue;
            long members = teamMemberRepository.countByTeamId(t.getId());
            if (members < MIN_TEAM_SIZE) {
                String reason = "Không đủ thành viên khi đóng đăng ký (" + members + "/" + MIN_TEAM_SIZE + ")";
                t.setStatus(TeamStatus.disqualified);
                t.setDisqualifiedReason(reason);
                writeTeamAudit(auth, t, AuditAction.DISQUALIFY_TEAM, TeamStatus.active.name(), TeamStatus.disqualified.name(), reason);
                eliminated++;
            }
        }
        e.setStatus(EventStatus.ongoing);
        if (timelineService != null) timelineService.record(e, null, null, null, "EVENT_REGISTRATION_CLOSED",
                TimelineScope.EVENT_PUBLIC, "Registration closed", "Event registration has closed",
                "EVENT_STATUS", e.getId(), "event:" + e.getId() + ":registration-closed");
        log.info("Event {} registration closed (published -> ongoing); {} teams registered, {} eliminated for being under {} members",
                e.getId(), teams.size(), eliminated, MIN_TEAM_SIZE);
        return toResponseWithCounts(e);
    }

    private void writeTeamAudit(Authentication auth, Team team, AuditAction action, String oldValue, String newValue, String details) {
        User actor = null;
        if (auth != null && auth.getPrincipal() instanceof CurrentUser c) {
            actor = userRepository.findById(c.getId()).orElse(null);
        }
        auditLogRepository.save(AuditLog.builder()
                .user(actor)
                .team(team)
                .action(action)
                .targetType("team")
                .targetId(team.getId())
                .oldValue(oldValue)
                .newValue(newValue)
                .details(details)
                .build());
    }

    /**
     * One-shot competition builder, run after registration closes (status=ongoing).
     * Creates thematic tracks, distributes registered teams round-robin, then for
     * each track generates R elimination rounds with an auto-computed topN funnel
     * and seeds round 1 with every team in that track.
     */
    @Transactional
    public SetupCompetitionResponse setupCompetition(UUID id, SetupCompetitionRequest req) {
        Event e = findOrThrow(id);
        if (e.getStatus() != EventStatus.ongoing) {
            throw ApiException.badRequest("Competition can only be set up after registration is closed (status must be ongoing, current: " + e.getStatus() + ")");
        }
        if (roundRepository.countByTrackEventId(e.getId()) > 0) {
            throw ApiException.conflict("Competition already set up for this event");
        }
        var seedReview = seedingService.setupReview(e.getId());
        List<EventSeedAssignment> fixedSeeds = seedingService.confirmedAssignments(e.getId());

        // Only active teams take part; under-strength teams were disqualified at registration close.
        List<Team> teams = teamRepository.findByTrackEventId(e.getId()).stream()
                .filter(t -> t.getStatus() == TeamStatus.active)
                .collect(java.util.stream.Collectors.toList());
        if (teams.isEmpty()) {
            throw ApiException.badRequest("No eligible (active) teams registered; cannot build the competition");
        }
        if (req == null || req.roundPlan() == null || req.roundPlan().isEmpty()) {
            throw ApiException.badRequest(
                    "roundPlan is required; each logical round must explicitly declare its track structure");
        }
        return setupFromLogicalRoundPlan(e, req, teams, seedReview, fixedSeeds);

        /*
         * Historical per-track funnel implementation retained below for source
         * traceability only. The explicit return above makes it unreachable: a
         * track-centric request cannot safely infer the event-level round model.
         */
        /*
        // 1) Resolve target tracks.
        List<Track> tracks = new ArrayList<>();
        boolean customTracks = req != null && req.tracks() != null && !req.tracks().isEmpty();
        if (customTracks) {
            Set<String> seen = new java.util.HashSet<>();
            for (SetupCompetitionRequest.TrackSpec spec : req.tracks()) {
                String name = spec.name().trim();
                if (!seen.add(name.toLowerCase())) {
                    throw ApiException.badRequest("Duplicate track name in request: " + name);
                }
                Track existing = trackRepository.findByEventId(e.getId(), org.springframework.data.domain.Pageable.unpaged())
                        .stream().filter(t -> t.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
                if (existing != null) {
                    tracks.add(existing);
                } else {
                    tracks.add(trackRepository.save(Track.builder()
                            .event(e).name(name).description(spec.description()).build()));
                }
            }
        } else {
            tracks.add(ensureGeneralTrack(e));
        }

        // 2) Preserve coordinator-confirmed/overridden seed tracks. All other
        // registrations are distributed round-robin. Seed metadata is preparation
        // metadata only and makes no bracket/group separation guarantee.
        Set<UUID> targetTrackIds = tracks.stream().map(Track::getId).collect(java.util.stream.Collectors.toSet());
        Map<UUID, EventSeedAssignment> fixedByTeam = fixedSeeds.stream()
                .collect(java.util.stream.Collectors.toMap(a -> a.getTeam().getId(), a -> a));
        for (EventSeedAssignment seed : fixedSeeds) {
            if (!targetTrackIds.contains(seed.getTrack().getId())
                    || !seed.getTeam().getTrack().getId().equals(seed.getTrack().getId())) {
                throw ApiException.conflict("Confirmed seed track must remain part of competition setup");
            }
        }
        int distributableIndex = 0;
        for (Team team : teams) {
            if (fixedByTeam.containsKey(team.getId())) continue;
            team.setTrack(tracks.get(distributableIndex++ % tracks.size()));
        }
        teamRepository.saveAll(teams);

        // 3) Per track: generate rounds + seed round 1.
        List<SetupCompetitionResponse.TrackPlan> trackPlans = new ArrayList<>();
        int roundsPerTrackReported = 0;
        for (Track track : tracks) {
            List<Team> trackTeams = teams.stream().filter(t -> t.getTrack().getId().equals(track.getId())).toList();
            int n = trackTeams.size();
            if (n == 0) {
                trackPlans.add(SetupCompetitionResponse.TrackPlan.builder()
                        .trackId(track.getId()).name(track.getName()).teamCount(0).rounds(List.of()).build());
                continue;
            }
            int f = resolveFinalists(req, n);
            int r = resolveRounds(req, n, f);
            roundsPerTrackReported = Math.max(roundsPerTrackReported, r);
            int[] funnel = computeFunnel(n, f, r);
            LocalDateTime[] deadlines = spreadDeadlines(e, r);

            List<SetupCompetitionResponse.RoundPlan> roundPlans = new ArrayList<>();
            for (int seq = 1; seq <= r; seq++) {
                final int sequence = seq;
                String logicalName = roundName(sequence, r);
                RoundDefinition definition = roundDefinitionRepository == null ? null
                        : roundDefinitionRepository.findByEventIdAndNameIgnoreCaseAndSequenceNumber(
                                e.getId(), logicalName, sequence).orElseGet(() -> roundDefinitionRepository.save(
                                        RoundDefinition.builder().event(e).name(logicalName).sequenceNumber(sequence).build()));
                Round round = roundRepository.save(Round.builder()
                        .logicalRound(definition)
                        .track(track)
                        .name(logicalName)
                        .sequenceNumber(seq)
                        .submissionDeadline(deadlines[seq - 1])
                        .topNToPromote(funnel[seq - 1])
                        .build());
                int seeded = 0;
                if (seq == 1) {
                    for (Team t : trackTeams) {
                        roundParticipantRepository.save(RoundParticipant.builder()
                                .round(round).team(t).status(RoundParticipantStatus.active).build());
                        seeded++;
                    }
                }
                roundPlans.add(SetupCompetitionResponse.RoundPlan.builder()
                        .roundId(round.getId()).name(round.getName()).sequenceNumber(seq)
                        .topNToPromote(funnel[seq - 1]).seededParticipants(seeded).build());
            }
            trackPlans.add(SetupCompetitionResponse.TrackPlan.builder()
                    .trackId(track.getId()).name(track.getName()).teamCount(n).rounds(roundPlans).build());
        }

        // 4) Drop an auto-created empty "General" track if the organiser used custom tracks.
        if (customTracks) {
            trackRepository.findByEventId(e.getId(), org.springframework.data.domain.Pageable.unpaged()).stream()
                    .filter(t -> t.getName().equalsIgnoreCase(GENERAL_TRACK))
                    .filter(t -> teams.stream().noneMatch(tm -> tm.getTrack().getId().equals(t.getId())))
                    .forEach(trackRepository::delete);
        }

        log.info("Event {} competition set up: {} teams, {} tracks", e.getId(), teams.size(), tracks.size());
        return SetupCompetitionResponse.builder()
                .eventId(e.getId())
                .totalTeams(teams.size())
                .trackCount(tracks.size())
                .roundsPerTrack(roundsPerTrackReported)
                .seedReview(seedReview)
                .warnings(seedReview.warning() == null ? List.of() : List.of(seedReview.warning()))
                .tracks(trackPlans)
                .build();
        */
    }

    private SetupCompetitionResponse setupFromLogicalRoundPlan(
            Event event,
            SetupCompetitionRequest request,
            List<Team> teams,
            vn.edu.fpt.seal.modules.seeding.dto.SeedingDtos.ReviewSummary seedReview,
            List<EventSeedAssignment> fixedSeeds) {
        if (roundDefinitionRepository == null) {
            throw ApiException.conflict("Logical-round setup is unavailable");
        }
        List<SetupCompetitionRequest.LogicalRoundSpec> plan = new ArrayList<>(request.roundPlan());
        plan.sort(java.util.Comparator.comparing(SetupCompetitionRequest.LogicalRoundSpec::sequenceNumber));
        Set<Integer> sequences = new java.util.HashSet<>();
        Set<String> names = new java.util.HashSet<>();
        Set<String> planTrackNames = new java.util.HashSet<>();
        int finalCount = 0;
        for (int index = 0; index < plan.size(); index++) {
            SetupCompetitionRequest.LogicalRoundSpec logical = plan.get(index);
            if (logical.sequenceNumber() == null || logical.sequenceNumber() != index + 1) {
                throw ApiException.badRequest("Logical-round sequences must be contiguous and start at 1");
            }
            if (!sequences.add(logical.sequenceNumber())
                    || !names.add(logical.name().trim().toLowerCase(java.util.Locale.ROOT))) {
                throw ApiException.badRequest("Logical-round names and sequences must be unique within the event");
            }
            if (logical.tracks() == null || logical.tracks().isEmpty()) {
                throw ApiException.badRequest("Every logical round must select at least one track");
            }
            boolean isFinal = Boolean.TRUE.equals(logical.finalRound());
            if (isFinal) finalCount++;
            if (isFinal && logical.tracks().size() != 1) {
                throw ApiException.badRequest("The final logical round must contain exactly one track execution");
            }
            if (isFinal && index != plan.size() - 1) {
                throw ApiException.badRequest("Only the last logical round may be final");
            }
            Set<String> trackNames = new java.util.HashSet<>();
            for (SetupCompetitionRequest.RoundTrackSpec track : logical.tracks()) {
                String normalizedTrackName = track.name().trim().toLowerCase(java.util.Locale.ROOT);
                if (!trackNames.add(normalizedTrackName)) {
                    throw ApiException.badRequest("A track may appear only once within a logical round");
                }
                if (!planTrackNames.add(normalizedTrackName)) {
                    throw ApiException.badRequest(
                            "Round-specific track names must be unique across the event round plan");
                }
            }
        }
        if (finalCount != 1) {
            throw ApiException.badRequest("The event round plan must contain exactly one final logical round");
        }

        Map<String, Track> tracksByName = trackRepository
                .findByEventId(event.getId(), org.springframework.data.domain.Pageable.unpaged()).stream()
                .collect(java.util.stream.Collectors.toMap(
                        track -> track.getName().toLowerCase(java.util.Locale.ROOT),
                        track -> track, (left, right) -> left, java.util.LinkedHashMap::new));
        for (SetupCompetitionRequest.LogicalRoundSpec logical : plan) {
            for (SetupCompetitionRequest.RoundTrackSpec trackSpec : logical.tracks()) {
                String key = trackSpec.name().trim().toLowerCase(java.util.Locale.ROOT);
                tracksByName.computeIfAbsent(key, ignored -> trackRepository.save(Track.builder()
                        .event(event).name(trackSpec.name().trim()).description(trackSpec.description()).build()));
            }
        }

        List<Track> firstRoundTracks = plan.get(0).tracks().stream()
                .map(spec -> tracksByName.get(spec.name().trim().toLowerCase(java.util.Locale.ROOT))).toList();
        Set<UUID> firstTrackIds = firstRoundTracks.stream().map(Track::getId).collect(java.util.stream.Collectors.toSet());
        Map<UUID, EventSeedAssignment> fixedByTeam = fixedSeeds.stream()
                .collect(java.util.stream.Collectors.toMap(a -> a.getTeam().getId(), a -> a));
        for (EventSeedAssignment seed : fixedSeeds) {
            if (!firstTrackIds.contains(seed.getTrack().getId())) {
                throw ApiException.conflict("Confirmed seed track must be selected in the first logical round");
            }
        }
        int distributableIndex = 0;
        for (Team team : teams) {
            EventSeedAssignment seed = fixedByTeam.get(team.getId());
            if (seed != null) {
                team.setTrack(seed.getTrack());
            } else {
                team.setTrack(firstRoundTracks.get(distributableIndex++ % firstRoundTracks.size()));
            }
        }
        teamRepository.saveAll(teams);

        LocalDateTime[] deadlines = spreadDeadlines(event, plan.size());
        Map<UUID, List<SetupCompetitionResponse.RoundPlan>> responseRounds = new java.util.LinkedHashMap<>();
        for (int index = 0; index < plan.size(); index++) {
            SetupCompetitionRequest.LogicalRoundSpec logical = plan.get(index);
            int defaultTopN = logical.defaultTopNToPromote() == null ? 1 : logical.defaultTopNToPromote();
            RoundDefinition definition = roundDefinitionRepository.save(RoundDefinition.builder()
                    .event(event)
                    .name(logical.name().trim())
                    .sequenceNumber(logical.sequenceNumber())
                    .finalRound(Boolean.TRUE.equals(logical.finalRound()))
                    .defaultTopNToPromote(defaultTopN)
                    .build());
            for (SetupCompetitionRequest.RoundTrackSpec trackSpec : logical.tracks()) {
                Track track = tracksByName.get(trackSpec.name().trim().toLowerCase(java.util.Locale.ROOT));
                int topN = trackSpec.topNToPromote() == null ? defaultTopN : trackSpec.topNToPromote();
                Round execution = roundRepository.save(Round.builder()
                        .logicalRound(definition).track(track)
                        .name(definition.getName()).sequenceNumber(definition.getSequenceNumber())
                        .submissionDeadline(deadlines[index]).topNToPromote(topN).build());
                int seeded = 0;
                if (index == 0) {
                    for (Team team : teams.stream()
                            .filter(candidate -> candidate.getTrack().getId().equals(track.getId())).toList()) {
                        roundParticipantRepository.save(RoundParticipant.builder()
                                .round(execution).team(team).status(RoundParticipantStatus.active).build());
                        seeded++;
                    }
                }
                responseRounds.computeIfAbsent(track.getId(), ignored -> new ArrayList<>())
                        .add(SetupCompetitionResponse.RoundPlan.builder()
                                .roundId(execution.getId()).name(definition.getName())
                                .sequenceNumber(definition.getSequenceNumber())
                                .topNToPromote(topN).seededParticipants(seeded).build());
            }
        }

        List<SetupCompetitionResponse.TrackPlan> responseTracks = tracksByName.values().stream()
                .filter(track -> responseRounds.containsKey(track.getId()))
                .map(track -> SetupCompetitionResponse.TrackPlan.builder()
                        .trackId(track.getId()).name(track.getName())
                        .teamCount((int) teams.stream()
                                .filter(team -> team.getTrack().getId().equals(track.getId())).count())
                        .rounds(responseRounds.get(track.getId())).build())
                .toList();
        return SetupCompetitionResponse.builder()
                .eventId(event.getId()).totalTeams(teams.size()).trackCount(responseTracks.size())
                .roundsPerTrack(plan.size()).seedReview(seedReview)
                .warnings(seedReview.warning() == null ? List.of() : List.of(seedReview.warning()))
                .tracks(responseTracks).build();
    }

    private Track ensureGeneralTrack(Event e) {
        return trackRepository.findByEventId(e.getId(), org.springframework.data.domain.Pageable.unpaged()).stream()
                .filter(t -> t.getName().equalsIgnoreCase(GENERAL_TRACK))
                .findFirst()
                .orElseGet(() -> trackRepository.save(Track.builder()
                        .event(e).name(GENERAL_TRACK).description("Default track for registered teams").build()));
    }

    /** Finalists per track: explicit value (capped to N), else max(3, ceil(0.1*N)) capped to N. */
    private int resolveFinalists(SetupCompetitionRequest req, int n) {
        int f = (req != null && req.finalistCount() != null)
                ? req.finalistCount()
                : Math.max(3, (int) Math.ceil(0.10 * n));
        return Math.min(Math.max(1, f), n);
    }

    /** Rounds per track: explicit value, else suggested by team count; clamped so funnel is valid. */
    private int resolveRounds(SetupCompetitionRequest req, int n, int f) {
        int r = (req != null && req.roundCount() != null) ? req.roundCount() : suggestRounds(n);
        // Can't eliminate anyone if N == F -> a single round. Otherwise at most (N - F) rounds make sense.
        int maxUseful = Math.max(1, n - f + 1);
        return Math.max(1, Math.min(r, maxUseful));
    }

    private int suggestRounds(int n) {
        if (n <= 8) return 1;
        if (n <= 30) return 2;
        if (n <= 80) return 3;
        return 4;
    }

    /**
     * Geometric elimination funnel: N -> ... -> F over R rounds. Returns topN per
     * round (length R), strictly decreasing when possible, last element == F.
     */
    private int[] computeFunnel(int n, int f, int r) {
        int[] out = new int[r];
        if (r == 1) {
            out[0] = f;
            return out;
        }
        double ratio = Math.pow((double) f / n, 1.0 / r);
        int prev = n;
        for (int i = 1; i <= r; i++) {
            int val = (i == r) ? f : (int) Math.round(n * Math.pow(ratio, i));
            if (val >= prev) val = prev - 1;      // enforce strictly decreasing
            if (val < f) val = f;                  // never drop below finalist target early
            out[i - 1] = Math.max(1, val);
            prev = out[i - 1];
        }
        out[r - 1] = f;
        return out;
    }

    /** Spread R submission deadlines across the event window (or from now if unset). */
    private LocalDateTime[] spreadDeadlines(Event e, int r) {
        LocalDateTime base = e.getEventStart() != null ? e.getEventStart() : LocalDateTime.now();
        LocalDateTime end = e.getEventEnd() != null && e.getEventEnd().isAfter(base)
                ? e.getEventEnd() : base.plusDays(Math.max(1, r) * 7L);
        long totalMinutes = ChronoUnit.MINUTES.between(base, end);
        long step = Math.max(1, totalMinutes / r);
        LocalDateTime[] out = new LocalDateTime[r];
        for (int i = 1; i <= r; i++) {
            out[i - 1] = base.plusMinutes(step * i);
        }
        return out;
    }

    private String roundName(int seq, int total) {
        if (total == 1) return "Final";
        if (seq == total) return "Final";
        if (seq == total - 1) return "Semifinal";
        return "Round " + seq;
    }

    private Event findOrThrow(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Event not found: " + id));
    }
}
