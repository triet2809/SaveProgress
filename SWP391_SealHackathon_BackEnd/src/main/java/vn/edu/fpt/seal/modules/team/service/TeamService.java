package vn.edu.fpt.seal.modules.team.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.enums.*;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.audit.entity.AuditLog;
import vn.edu.fpt.seal.modules.audit.repository.AuditLogRepository;
import vn.edu.fpt.seal.modules.team.dto.*;
import vn.edu.fpt.seal.modules.team.entity.*;
import vn.edu.fpt.seal.modules.team.mapper.TeamMapper;
import vn.edu.fpt.seal.modules.team.repository.*;
import vn.edu.fpt.seal.security.CurrentUser;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.track.repository.TrackRepository;
import vn.edu.fpt.seal.modules.user.entity.User;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.modules.event.repository.EventRepository;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.teamprofile.entity.TeamProfile;
import vn.edu.fpt.seal.modules.teamprofile.repository.TeamProfileRepository;
import vn.edu.fpt.seal.modules.recognition.service.TeamRecognitionService;
import vn.edu.fpt.seal.modules.timeline.TimelineScope;
import vn.edu.fpt.seal.modules.timeline.service.TimelineService;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {
    /** Hackathon teams must have between MIN and MAX members (requirement #2). */
    private static final int MIN_TEAM_SIZE = 3;
    private static final int MAX_TEAM_SIZE = 5;

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TrackRepository trackRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final EventRepository eventRepository;
    private final TeamProfileRepository teamProfileRepository;
    private final TeamRecognitionService recognitionService;
    private final TimelineService timelineService;

    @Transactional(readOnly = true)
    public Page<TeamResponse> listByTrack(UUID eventId, UUID trackId, Pageable pageable) {
        if (eventId == null) throw ApiException.badRequest("eventId is required");
        if (!eventRepository.existsById(eventId)) throw ApiException.notFound("Event not found: " + eventId);
        if (trackId != null && !trackRepository.findById(trackId)
                .filter(t -> t.getEvent().getId().equals(eventId)).isPresent()) {
            throw ApiException.badRequest("Track does not belong to the selected event");
        }
        Page<Team> teams = trackId == null
                ? teamRepository.findByTrackEventId(eventId, pageable)
                : teamRepository.findByTrackEventIdAndTrackId(eventId, trackId, pageable);
        Map<UUID, List<vn.edu.fpt.seal.modules.recognition.dto.RecognitionDtos.Summary>> recognitionByTeam =
                recognitionService.activeByTeamIds(teams.getContent().stream().map(Team::getId).toList());
        return teams.map(t -> toResponse(t, false,
                recognitionByTeam.getOrDefault(t.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public TeamResponse get(UUID id, Authentication auth) {
        Team team = findOrThrow(id);
        boolean coordinator = isCoordinator(auth);
        boolean member = auth != null && auth.getPrincipal() instanceof CurrentUser c
                && teamMemberRepository.existsByTeamIdAndUserId(team.getId(), c.getId());
        return toResponse(team, coordinator || member);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> myTeams(Authentication auth) {
        UUID callerId = currentUserId(auth);
        List<Team> teams = teamMemberRepository.findByUserIdOrderByJoinedAtDesc(callerId).stream()
                .map(TeamMember::getTeam)
                .distinct()
                .sorted(Comparator.comparingInt(this::currentRegistrationPriority)
                        .thenComparing(Team::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        Map<UUID, List<vn.edu.fpt.seal.modules.recognition.dto.RecognitionDtos.Summary>> recognitionByTeam =
                recognitionService.activeByTeamIds(teams.stream().map(Team::getId).toList());
        return teams.stream().map(team -> toResponse(team, true,
                recognitionByTeam.getOrDefault(team.getId(), List.of()))).toList();
    }

    @Transactional
    public TeamResponse create(CreateTeamRequest req, Authentication auth) {
        Event event = eventRepository.findById(req.eventId())
                .orElseThrow(() -> ApiException.notFound("Event not found: " + req.eventId()));
        ensureRegistrationOpen(event);
        String name = req.name().trim();
        if (teamRepository.existsByEventIdAndTrackIsNullAndNameIgnoreCase(event.getId(), name))
            throw ApiException.conflict("An unassigned team with this name already exists in this event");

        boolean coordinator = isCoordinator(auth);
        UUID actorId = currentUserIdOrNull(auth);
        User creator = actorId == null ? null : userRepository.findById(actorId).orElse(null);
        TeamProfile profile = teamProfileRepository.save(TeamProfile.builder()
                .canonicalName(name).createdBy(creator).status(TeamProfileStatus.active).build());
        Team team = teamRepository.save(Team.builder().teamProfile(profile).event(event).track(null).name(name)
                .status(TeamStatus.active).inviteCode(generateInviteCode()).build());
        timelineService.record(event, team.getId(), null, null, "TEAM_CREATED",
                TimelineScope.EVENT_PARTICIPANTS, "Team created", "A team registration was created",
                "TEAM", team.getId(), "team:" + team.getId() + ":created");
        Set<UUID> added = new LinkedHashSet<>();

        if (coordinator) {
            // Coordinator may create on behalf of others; leader/members optional
            // (they can build the roster incrementally). Hard cap at MAX_TEAM_SIZE.
            if (req.leaderUserId() != null) { addMemberInternal(team, req.leaderUserId(), TeamMemberRole.leader); added.add(req.leaderUserId()); }
            if (req.memberUserIds() != null) for (UUID id : req.memberUserIds()) if (added.add(id)) addMemberInternal(team, id, TeamMemberRole.member);
            for (UUID mid : resolveEmails(req.memberEmails())) if (added.add(mid)) addMemberInternal(team, mid, TeamMemberRole.member);
            if (added.size() > MAX_TEAM_SIZE) throw ApiException.badRequest("A team can have at most " + MAX_TEAM_SIZE + " members");
        } else {
            // A regular (non-coordinator) user creating their own team becomes the
            // leader. Solo creation is allowed (size 1); the roster grows later via
            // invite code or accepted join requests. Minimum size is enforced at a
            // later gate (registration close / submission), not at creation time.
            UUID callerId = currentUserId(auth);
            addMemberInternal(team, callerId, TeamMemberRole.leader); added.add(callerId);
            if (req.memberUserIds() != null) for (UUID id : req.memberUserIds()) if (added.add(id)) addMemberInternal(team, id, TeamMemberRole.member);
            for (UUID mid : resolveEmails(req.memberEmails())) if (added.add(mid)) addMemberInternal(team, mid, TeamMemberRole.member);
            if (added.size() > MAX_TEAM_SIZE)
                throw ApiException.badRequest("A team can have at most " + MAX_TEAM_SIZE + " members (including the leader)");
        }
        log.info("Team created unassigned: id={}, event={}, name={}, byCoordinator={}",
                team.getId(), event.getId(), team.getName(), coordinator);
        return toResponse(team);
    }

    /** Resolve member emails to user ids; each must be an existing registered user. */
    private List<UUID> resolveEmails(List<String> emails) {
        if (emails == null) return List.of();
        List<UUID> ids = new ArrayList<>();
        for (String raw : emails) {
            if (raw == null || raw.isBlank()) continue;
            String email = raw.toLowerCase().trim();
            User u = userRepository.findByEmail(email).orElseThrow(() -> ApiException.badRequest("No registered user with email: " + email));
            ids.add(u.getId());
        }
        return ids;
    }

    /** Generate a unique 6-char uppercase invite code. */
    private String generateInviteCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
            if (!teamRepository.existsByInviteCode(code)) return code;
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    @Transactional
    public TeamResponse update(UUID id, UpdateTeamRequest req) {
        Team team = findOrThrow(id); ensureEditable(team.getEvent());
        if (req.name() != null) {
            String name = req.name().trim();
            boolean duplicate = team.getTrack() == null
                    ? teamRepository.existsByEventIdAndTrackIsNullAndNameIgnoreCase(team.getEvent().getId(), name)
                    : teamRepository.existsByTrackIdAndNameIgnoreCase(team.getTrack().getId(), name);
            if (!name.equalsIgnoreCase(team.getName()) && duplicate)
                throw ApiException.conflict("Team name already exists in this allocation scope");
            team.setName(name);
        }
        return toResponse(team);
    }

    @Transactional
    public TeamResponse moveToTrack(UUID id, MoveTeamTrackRequest req, Authentication auth) {
        Team team = findOrThrow(id);
        Track target = trackRepository.findById(req.trackId()).orElseThrow(() -> ApiException.notFound("Track not found: " + req.trackId()));
        Track current = team.getTrack();
        if (current != null && current.getId().equals(target.getId())) return toResponse(team);
        // Target track must be in the same event (cross-event moves are not allowed).
        if (!team.getEvent().getId().equals(target.getEvent().getId()))
            throw ApiException.badRequest("Target track must belong to the same event");
        ensureEditable(target.getEvent());
        // Name must stay unique within the destination track.
        if (teamRepository.existsByTrackIdAndNameIgnoreCase(target.getId(), team.getName()))
            throw ApiException.conflict("A team with this name already exists in the target track");
        String oldTrack = current == null ? "unassigned" : current.getId().toString();
        team.setTrack(target);
        // Requirement #10-style audit trail: record cross-track moves.
        writeAudit(auth, team, AuditAction.PROMOTE_TEAM, oldTrack, target.getId().toString(), "Moved team to track " + target.getName());
        log.info("Team moved: id={}, from track={}, to track={}", team.getId(), oldTrack, target.getId());
        return toResponse(team);
    }

    @Transactional
    public TeamResponse joinByInviteCode(JoinTeamRequest req, Authentication auth) {
        UUID callerId = currentUserId(auth);
        String code = req.inviteCode().trim().toUpperCase().replace("SEAL-", "").replace("-", "");
        Team team = teamRepository.findByInviteCodeIgnoreCase(code)
                .orElseThrow(() -> ApiException.notFound("Team invite code not found"));
        ensureEditable(team.getEvent());
        ensureRegistrationOpen(team.getEvent());
        if (teamMemberRepository.existsByTeamIdAndUserId(team.getId(), callerId)) return toResponse(team);
        if (teamMemberRepository.countByTeamId(team.getId()) >= MAX_TEAM_SIZE) throw ApiException.badRequest("A team can have at most " + MAX_TEAM_SIZE + " members");
        addMemberInternal(team, callerId, TeamMemberRole.member);
        return toResponse(team);
    }

    @Transactional
    public TeamResponse addMember(UUID teamId, AddTeamMemberRequest req) {
        Team team = findOrThrow(teamId); ensureEditable(team.getEvent());
        if (teamMemberRepository.countByTeamId(teamId) >= MAX_TEAM_SIZE)
            throw ApiException.badRequest("A team can have at most " + MAX_TEAM_SIZE + " members");
        addMemberInternal(team, req.userId(), req.role() == null ? TeamMemberRole.member : req.role());
        return toResponse(team);
    }

    @Transactional
    public void removeMember(UUID teamId, UUID userId) {
        Team team = findOrThrow(teamId); ensureEditable(team.getEvent());
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId).orElseThrow(() -> ApiException.notFound("Team member not found"));
        teamMemberRepository.delete(member);
    }

    @Transactional
    public TeamResponse disqualify(UUID id, DisqualifyTeamRequest req, Authentication auth) {
        Team team = findOrThrow(id);
        String oldStatus = team.getStatus().name();
        team.setStatus(TeamStatus.disqualified);
        team.setDisqualifiedReason(req.reason().trim());
        // Requirement #10: disqualification must leave an audit trail.
        writeAudit(auth, team, AuditAction.DISQUALIFY_TEAM, oldStatus, TeamStatus.disqualified.name(), req.reason().trim());
        timelineService.record(team.getEvent(), team.getId(), null,
                team.getTrack() == null ? null : team.getTrack().getId(),
                "TEAM_DISQUALIFIED", TimelineScope.EVENT_PARTICIPANTS, "Team disqualified",
                "The team was disqualified", "TEAM", team.getId(),
                "team:" + team.getId() + ":disqualified:" + team.getUpdatedAt());
        return toResponse(team);
    }

    @Transactional
    public TeamResponse reactivate(UUID id, Authentication auth) {
        Team team = findOrThrow(id);
        ensureEditable(team.getEvent());
        if (team.getStatus() == TeamStatus.active) {
            throw ApiException.conflict("Team is already active");
        }
        if (team.getStatus() != TeamStatus.disqualified) {
            throw ApiException.badRequest("Team status cannot be reactivated: " + team.getStatus());
        }
        if (team.getTeamProfile() == null || team.getTeamProfile().getStatus() != TeamProfileStatus.active) {
            throw ApiException.badRequest("Team is not linked to an active team profile");
        }
        String oldStatus = team.getStatus().name();
        team.setStatus(TeamStatus.active);
        team.setDisqualifiedReason(null);
        team = teamRepository.saveAndFlush(team);
        writeAudit(auth, team, AuditAction.UPDATE, oldStatus, TeamStatus.active.name(),
                "Coordinator restored this event registration before registration closed");
        timelineService.record(team.getEvent(), team.getId(), null,
                team.getTrack() == null ? null : team.getTrack().getId(),
                "TEAM_REACTIVATED", TimelineScope.EVENT_PARTICIPANTS, "Team reactivated",
                "The team registration was reactivated", "TEAM", team.getId(),
                "team:" + team.getId() + ":reactivated:" + team.getUpdatedAt());
        return toResponse(team);
    }

    @Transactional
    public void delete(UUID id) { Team team = findOrThrow(id); ensureDraft(team.getEvent()); teamRepository.delete(team); }

    private TeamResponse toResponse(Team team) { return toResponse(team, true); }
    private TeamResponse toResponse(Team team, boolean includeInviteCode) {
        return toResponse(team, includeInviteCode,
                recognitionService.activeByTeamIds(List.of(team.getId()))
                        .getOrDefault(team.getId(), List.of()));
    }
    private TeamResponse toResponse(
            Team team, boolean includeInviteCode,
            List<vn.edu.fpt.seal.modules.recognition.dto.RecognitionDtos.Summary> recognitions) {
        return TeamMapper.toResponse(team,
                teamMemberRepository.findByTeamIdOrderByRoleAscJoinedAtAsc(team.getId()),
                includeInviteCode, recognitions);
    }
    private Team findOrThrow(UUID id) { return teamRepository.findWithTrackById(id).orElseThrow(() -> ApiException.notFound("Team not found: " + id)); }
    private TeamMember addMemberInternal(Team team, UUID userId, TeamMemberRole role) {
        if (teamMemberRepository.existsByTeamIdAndUserId(team.getId(), userId)) throw ApiException.conflict("User already belongs to this team");
        if (teamMemberRepository.existsActiveRegistrationInEvent(userId, team.getEvent().getId()))
            throw ApiException.conflict("User already belongs to another active team in this event");
        if (role == TeamMemberRole.leader && teamMemberRepository.existsByTeamIdAndRole(team.getId(), TeamMemberRole.leader)) throw ApiException.conflict("Team already has a leader");
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found: " + userId));
        if (user.getStatus() != AccountStatus.approved) throw ApiException.badRequest("Only approved users can join teams");
        return teamMemberRepository.save(TeamMember.builder().team(team).user(user).role(role).build());
    }
    private void ensureEditable(Event event) { EventStatus s = event.getStatus(); if (s != EventStatus.draft && s != EventStatus.published) throw ApiException.badRequest("Historical team registrations cannot be edited after registration closes (status: " + s + ")"); }
    /** Team registration (create/join) is only allowed while the event has registration open (status=published). */
    private void ensureRegistrationOpen(Event event) { EventStatus s = event.getStatus(); if (s != EventStatus.published) throw ApiException.badRequest("Registration is not open for this event (status: " + s + ")"); }
    private void ensureDraft(Event event) { EventStatus s = event.getStatus(); if (s != EventStatus.draft) throw ApiException.badRequest("Teams can only be deleted while event is draft (current: " + s + ")"); }

    private boolean isCoordinator(Authentication auth) {
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_COORDINATOR"));
    }
    private UUID currentUserId(Authentication auth) {
        UUID id = currentUserIdOrNull(auth);
        if (id == null) throw ApiException.forbidden("Authentication required");
        return id;
    }
    private UUID currentUserIdOrNull(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof CurrentUser c) return c.getId();
        return null;
    }
    private void writeAudit(Authentication auth, Team team, AuditAction action, String oldValue, String newValue, String details) {
        UUID actorId = currentUserIdOrNull(auth);
        User actor = actorId == null ? null : userRepository.findById(actorId).orElse(null);
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

    private int currentRegistrationPriority(Team team) {
        if (team.getStatus() == TeamStatus.disqualified) return 10;
        return switch (team.getEvent().getStatus()) {
            case ongoing -> 0;
            case published -> 1;
            case draft -> 2;
            case completed -> 3;
            case cancelled -> 4;
        };
    }
}
