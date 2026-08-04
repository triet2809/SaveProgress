package vn.edu.fpt.seal.modules.teamprofile.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.enums.*;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.audit.entity.AuditLog;
import vn.edu.fpt.seal.modules.audit.repository.AuditLogRepository;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.event.repository.EventRepository;
import vn.edu.fpt.seal.modules.recognition.service.TeamRecognitionService;
import vn.edu.fpt.seal.modules.team.dto.TeamResponse;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.team.entity.TeamMember;
import vn.edu.fpt.seal.modules.team.mapper.TeamMapper;
import vn.edu.fpt.seal.modules.team.repository.TeamMemberRepository;
import vn.edu.fpt.seal.modules.team.repository.TeamRepository;
import vn.edu.fpt.seal.modules.teamprofile.dto.TeamProfileDtos;
import vn.edu.fpt.seal.modules.teamprofile.entity.TeamProfile;
import vn.edu.fpt.seal.modules.teamprofile.repository.TeamProfileRepository;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.track.repository.TrackRepository;
import vn.edu.fpt.seal.modules.user.entity.User;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.security.CurrentUser;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class TeamProfileService {
    private static final int MIN_RETURNING_MEMBERS = 3;
    private static final int MAX_TEAM_SIZE = 5;

    private final TeamProfileRepository profiles;
    private final TeamRepository teams;
    private final TeamMemberRepository members;
    private final TrackRepository tracks;
    private final EventRepository events;
    private final UserRepository users;
    private final AuditLogRepository audits;
    private final TeamRecognitionService recognitionService;

    public TeamProfileService(TeamProfileRepository profiles, TeamRepository teams,
                              TeamMemberRepository members, TrackRepository tracks,
                              EventRepository events, UserRepository users,
                              AuditLogRepository audits) {
        this.profiles = profiles;
        this.teams = teams;
        this.members = members;
        this.tracks = tracks;
        this.events = events;
        this.users = users;
        this.audits = audits;
        this.recognitionService = null;
    }

    @Transactional(readOnly = true)
    public List<TeamProfileDtos.ProfileSummary> mine(UUID targetEventId, Authentication authentication) {
        UUID callerId = current(authentication).getId();
        List<TeamMember> callerMemberships = members.findByUserIdOrderByJoinedAtDesc(callerId);
        Set<UUID> profileIds = callerMemberships.stream()
                .filter(member -> isHistorical(member.getTeam().getTrack().getEvent()))
                .map(TeamMember::getTeam)
                .map(Team::getTeamProfile)
                .filter(Objects::nonNull)
                .map(TeamProfile::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (profileIds.isEmpty()) return List.of();

        Map<UUID, TeamProfile> profileById = profiles.findAllById(profileIds).stream()
                .collect(Collectors.toMap(TeamProfile::getId, Function.identity()));
        List<Team> registrations = teams.findByTeamProfileIdIn(profileIds);
        Map<UUID, List<TeamMember>> rosterByTeam = members.findByTeamIdIn(
                        registrations.stream().map(Team::getId).toList()).stream()
                .collect(Collectors.groupingBy(tm -> tm.getTeam().getId()));
        Map<UUID, List<Team>> teamsByProfile = registrations.stream()
                .collect(Collectors.groupingBy(t -> t.getTeamProfile().getId()));
        Map<UUID, List<vn.edu.fpt.seal.modules.recognition.dto.RecognitionDtos.Summary>>
                recognitionsByProfile = recognitionService == null ? Map.of()
                : recognitionService.activeByProfileIds(profileIds);

        return profileIds.stream().map(profileId -> {
            TeamProfile profile = profileById.get(profileId);
            List<Team> profileTeams = teamsByProfile.getOrDefault(profileId, List.of()).stream()
                    .sorted(Comparator.comparing(Team::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
            boolean eligible = profileTeams.stream().anyMatch(team ->
                    isHistorical(team.getTrack().getEvent())
                            && rosterByTeam.getOrDefault(team.getId(), List.of()).stream()
                            .anyMatch(member -> member.getUser().getId().equals(callerId)
                                    && member.getRole() == TeamMemberRole.leader));
            boolean registered = targetEventId != null && profileTeams.stream()
                    .anyMatch(team -> team.getTrack().getEvent().getId().equals(targetEventId));
            List<TeamProfileDtos.HistoricalRegistration> history = profileTeams.stream()
                    .map(team -> historicalRegistration(team, callerId,
                            rosterByTeam.getOrDefault(team.getId(), List.of())))
                    .toList();
            return new TeamProfileDtos.ProfileSummary(profileId, profile.getCanonicalName(),
                    profile.getLogoUrl(), recognitionsByProfile.getOrDefault(profileId, List.of()),
                    eligible, registered, history);
        }).toList();
    }

    @Transactional(readOnly = true)
    public TeamProfileDtos.PreviewResponse preview(UUID profileId,
                                                   TeamProfileDtos.ReactivationRequest request,
                                                   Authentication authentication) {
        TeamProfile profile = profiles.findById(profileId)
                .orElseThrow(() -> ApiException.notFound("Team profile not found: " + profileId));
        return validate(profile, request, authentication);
    }

    @Transactional
    public TeamResponse reactivate(UUID profileId,
                                   TeamProfileDtos.ReactivationRequest request,
                                   Authentication authentication) {
        TeamProfile profile = profiles.findByIdForUpdate(profileId)
                .orElseThrow(() -> ApiException.notFound("Team profile not found: " + profileId));
        TeamProfileDtos.PreviewResponse preview = validate(profile, request, authentication);
        if (!preview.eligible()) {
            throw ApiException.conflict(String.join("; ",
                    preview.missingRequirements().isEmpty()
                            ? preview.memberConflicts().stream().map(TeamProfileDtos.MemberConflict::reason).toList()
                            : preview.missingRequirements()));
        }

        Team source = team(request.sourceTeamId());
        Track target = track(request.targetTrackId());
        User actor = users.findById(current(authentication).getId()).orElseThrow();
        LocalDateTime now = LocalDateTime.now();
        Team created = Team.builder()
                .teamProfile(profile)
                .sourceTeam(source)
                .track(target)
                .name(profile.getCanonicalName())
                .status(TeamStatus.active)
                .inviteCode(generateInviteCode())
                .activatedFromProfileAt(now)
                .rosterConfirmedAt(now)
                .rosterConfirmedBy(actor)
                .build();
        try {
            created = teams.saveAndFlush(created);
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("Team profile already has a registration in the target event");
        }

        Map<UUID, TeamProfileDtos.ProposedMember> proposed = preview.proposedNewRoster().stream()
                .collect(Collectors.toMap(TeamProfileDtos.ProposedMember::userId, Function.identity()));
        List<TeamMember> createdMembers = new ArrayList<>();
        for (UUID userId : request.returningMemberIds()) {
            User user = users.findById(userId)
                    .orElseThrow(() -> ApiException.notFound("User not found: " + userId));
            TeamMemberRole role = proposed.get(userId).role();
            createdMembers.add(members.save(TeamMember.builder().team(created).user(user).role(role).build()));
        }

        audits.save(AuditLog.builder()
                .user(actor).team(created).action(AuditAction.CREATE)
                .targetType("team").targetId(created.getId())
                .oldValue(source.getId().toString()).newValue(created.getId().toString())
                .details("Reactivated team profile " + profile.getId() + " for event "
                        + target.getEvent().getId() + " with " + createdMembers.size() + " returning members")
                .build());
        var recognitionSummaries = recognitionService == null ? List.<vn.edu.fpt.seal.modules.recognition.dto.RecognitionDtos.Summary>of()
                : recognitionService.activeByProfileIds(List.of(profile.getId()))
                .getOrDefault(profile.getId(), List.of());
        return TeamMapper.toResponse(created, sortRoster(createdMembers), true, recognitionSummaries);
    }

    private TeamProfileDtos.PreviewResponse validate(TeamProfile profile,
                                                     TeamProfileDtos.ReactivationRequest request,
                                                     Authentication authentication) {
        CurrentUser caller = current(authentication);
        Team source = team(request.sourceTeamId());
        if (source.getTeamProfile() == null || !source.getTeamProfile().getId().equals(profile.getId())) {
            throw ApiException.badRequest("Source team does not belong to the selected profile");
        }
        List<TeamMember> historicalRoster = members.findByTeamIdOrderByRoleAscJoinedAtAsc(source.getId());
        TeamMember callerMembership = historicalRoster.stream()
                .filter(member -> member.getUser().getId().equals(caller.getId()))
                .findFirst().orElseThrow(() -> ApiException.forbidden("Requester did not belong to the source team"));
        if (callerMembership.getRole() != TeamMemberRole.leader) {
            throw ApiException.forbidden("Only the historical team leader may initiate reactivation");
        }

        Event targetEvent = events.findById(request.targetEventId())
                .orElseThrow(() -> ApiException.notFound("Target event not found: " + request.targetEventId()));
        Track targetTrack = track(request.targetTrackId());
        if (!targetTrack.getEvent().getId().equals(targetEvent.getId())) {
            throw ApiException.badRequest("Target track does not belong to the target event");
        }
        if (source.getTrack().getEvent().getId().equals(targetEvent.getId())) {
            throw ApiException.badRequest("Source and target events must be different");
        }

        LinkedHashSet<UUID> returningIds = new LinkedHashSet<>(request.returningMemberIds());
        Map<UUID, TeamMember> historicalByUser = historicalRoster.stream()
                .collect(Collectors.toMap(member -> member.getUser().getId(), Function.identity()));
        List<String> missing = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        Set<UUID> invalidMembers = returningIds.stream()
                .filter(id -> !historicalByUser.containsKey(id))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!invalidMembers.isEmpty()) missing.add("All returning members must belong to the source historical roster");
        if (returningIds.size() < MIN_RETURNING_MEMBERS) {
            missing.add("At least three historical members must return");
        }
        if (returningIds.size() > MAX_TEAM_SIZE) missing.add("A team can have at most five members");
        if (!returningIds.contains(request.leaderId())) missing.add("The selected leader must be returning");
        if (!historicalByUser.containsKey(request.leaderId()))
            missing.add("The selected leader must belong to the source roster");
        if (returningIds.stream().filter(historicalByUser::containsKey)
                .map(historicalByUser::get).map(TeamMember::getUser)
                .anyMatch(user -> user.getStatus() != AccountStatus.approved)) {
            missing.add("All returning members must use approved existing accounts");
        }
        if (!isHistorical(source.getTrack().getEvent())) missing.add("Source registration is not historical yet");
        if (targetEvent.getStatus() != EventStatus.published) missing.add("Target event registration is not open");
        if (profile.getStatus() != TeamProfileStatus.active) missing.add("Team profile is not active");
        if (teams.existsByTeamProfileIdAndTrackEventId(profile.getId(), targetEvent.getId())) {
            missing.add("Team profile already has a registration in the target event");
        }
        if (targetTrack.getMaxTeams() != null && teams.countByTrackId(targetTrack.getId()) >= targetTrack.getMaxTeams()) {
            missing.add("Target track has reached its team capacity");
        }

        List<TeamMember> conflicts = returningIds.isEmpty() ? List.of()
                : members.findActiveRegistrationsInEvent(returningIds, targetEvent.getId(), vn.edu.fpt.seal.common.enums.TeamStatus.active);
        List<TeamProfileDtos.MemberConflict> memberConflicts = conflicts.stream()
                .map(member -> new TeamProfileDtos.MemberConflict(member.getUser().getId(),
                        member.getUser().getFullName(), member.getTeam().getId(),
                        "Member already belongs to active team " + member.getTeam().getName()
                                + " in the target event"))
                .toList();
        if (!memberConflicts.isEmpty()) warnings.add("Resolve target-event team conflicts before confirming");

        List<TeamProfileDtos.HistoricalMember> roster = historicalRoster.stream()
                .map(this::historicalMember).toList();
        List<TeamProfileDtos.ProposedMember> proposed = returningIds.stream()
                .filter(historicalByUser::containsKey)
                .map(id -> {
                    TeamMember member = historicalByUser.get(id);
                    TeamMemberRole role = id.equals(request.leaderId())
                            ? TeamMemberRole.leader : TeamMemberRole.member;
                    return new TeamProfileDtos.ProposedMember(id, member.getUser().getFullName(), role);
                }).toList();
        boolean eligible = missing.isEmpty() && memberConflicts.isEmpty();
        return new TeamProfileDtos.PreviewResponse(eligible, returningIds.size(),
                memberConflicts, missing, roster, proposed, warnings);
    }

    private TeamProfileDtos.HistoricalRegistration historicalRegistration(
            Team team, UUID callerId, List<TeamMember> roster) {
        TeamMemberRole callerRole = roster.stream()
                .filter(member -> member.getUser().getId().equals(callerId))
                .map(TeamMember::getRole).findFirst().orElse(null);
        return new TeamProfileDtos.HistoricalRegistration(team.getId(),
                team.getTrack().getEvent().getId(), team.getTrack().getEvent().getTitle(),
                team.getTrack().getEvent().getStatus().name(), team.getTrack().getId(),
                team.getTrack().getName(), callerRole,
                roster.stream().map(this::historicalMember).toList());
    }

    private TeamProfileDtos.HistoricalMember historicalMember(TeamMember member) {
        return new TeamProfileDtos.HistoricalMember(member.getUser().getId(),
                member.getUser().getFullName(), member.getRole(), member.getJoinedAt());
    }

    private List<TeamMember> sortRoster(List<TeamMember> roster) {
        return roster.stream().sorted(Comparator.comparing(TeamMember::getRole)
                .thenComparing(TeamMember::getJoinedAt, Comparator.nullsLast(Comparator.naturalOrder()))).toList();
    }

    private Team team(UUID id) {
        return teams.findWithTrackById(id)
                .orElseThrow(() -> ApiException.notFound("Team not found: " + id));
    }

    private Track track(UUID id) {
        return tracks.findById(id)
                .orElseThrow(() -> ApiException.notFound("Track not found: " + id));
    }

    private boolean isHistorical(Event event) {
        return event.getStatus() == EventStatus.ongoing
                || event.getStatus() == EventStatus.completed
                || event.getStatus() == EventStatus.cancelled;
    }

    private String generateInviteCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
            if (!teams.existsByInviteCode(code)) return code;
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private CurrentUser current(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser current)) {
            throw ApiException.unauthorized("Authentication required");
        }
        return current;
    }
}
