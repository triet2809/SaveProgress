package vn.edu.fpt.seal.modules.recognition.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.enums.AuditAction;
import vn.edu.fpt.seal.common.enums.EventStatus;
import vn.edu.fpt.seal.common.enums.TeamStatus;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.audit.entity.AuditLog;
import vn.edu.fpt.seal.modules.audit.repository.AuditLogRepository;
import vn.edu.fpt.seal.modules.recognition.dto.RecognitionDtos;
import vn.edu.fpt.seal.modules.recognition.entity.TeamRecognition;
import vn.edu.fpt.seal.modules.recognition.repository.TeamRecognitionRepository;
import vn.edu.fpt.seal.modules.seeding.entity.EventTeamFinish;
import vn.edu.fpt.seal.modules.seeding.repository.EventTeamFinishRepository;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.team.repository.TeamRepository;
import vn.edu.fpt.seal.modules.teamprofile.entity.TeamProfile;
import vn.edu.fpt.seal.modules.teamprofile.repository.TeamProfileRepository;
import vn.edu.fpt.seal.modules.timeline.TimelineEventType;
import vn.edu.fpt.seal.modules.timeline.TimelineScope;
import vn.edu.fpt.seal.modules.timeline.TimelineSourceType;
import vn.edu.fpt.seal.modules.timeline.dto.TimelineEventRequest;
import vn.edu.fpt.seal.modules.timeline.service.TimelineService;
import vn.edu.fpt.seal.modules.user.entity.User;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.security.CurrentUser;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamRecognitionService {
    public static final String VETERAN_CODE = "HACKATHON_VETERAN";
    public static final String VETERAN_LABEL = "Hackathon Veteran";
    public static final int REQUIRED_SEASONS = 3;

    private final TeamRecognitionRepository recognitions;
    private final EventTeamFinishRepository finishes;
    private final TeamProfileRepository profiles;
    private final UserRepository users;
    private final AuditLogRepository audits;
    @Autowired
    private TimelineService timeline;
    @Autowired
    private TeamRepository teams;

    @Transactional
    public Map<UUID, RecognitionDtos.Summary> evaluateProfiles(Collection<UUID> profileIds, User actor) {
        if (profileIds == null || profileIds.isEmpty()) return Map.of();
        Map<UUID, TeamProfile> profileById = profiles.findAllById(new LinkedHashSet<>(profileIds)).stream()
                .collect(Collectors.toMap(TeamProfile::getId, Function.identity()));
        Map<UUID, RecognitionDtos.Summary> result = new LinkedHashMap<>();
        for (UUID profileId : new LinkedHashSet<>(profileIds)) {
            TeamProfile profile = profileById.get(profileId);
            if (profile == null) continue;
            long qualifyingCount = finishes.countDistinctQualifyingEvents(
                    profileId, EventStatus.completed, TeamStatus.disqualified);
            TeamRecognition recognition = recognitions
                    .findFirstByTeamProfileIdAndRecognitionCodeOrderByCreatedAtDesc(profileId, VETERAN_CODE)
                    .orElse(null);

            if (recognition == null && qualifyingCount < REQUIRED_SEASONS) continue;
            boolean firstAward = recognition == null;
            if (firstAward) {
                recognition = TeamRecognition.builder()
                        .teamProfile(profile)
                        .recognitionCode(VETERAN_CODE)
                        .label(VETERAN_LABEL)
                        .qualificationCount(Math.toIntExact(qualifyingCount))
                        .earnedAt(LocalDateTime.now())
                        .active(true)
                        .build();
            } else {
                recognition.setQualificationCount(Math.toIntExact(qualifyingCount));
                recognition.setLabel(VETERAN_LABEL);
            }
            try {
                recognition = recognitions.saveAndFlush(recognition);
            } catch (DataIntegrityViolationException ex) {
                if (!isActiveRecognitionConflict(ex)) throw ex;
                recognition = recognitions
                        .findFirstByTeamProfileIdAndRecognitionCodeOrderByCreatedAtDesc(profileId, VETERAN_CODE)
                        .orElseThrow(() -> ex);
                firstAward = false;
            }
            if (firstAward) {
                audits.save(AuditLog.builder()
                        .user(actor)
                        .action(AuditAction.TEAM_RECOGNITION_AWARDED)
                        .targetType("team_recognition")
                        .targetId(recognition.getId())
                        .newValue(VETERAN_CODE)
                        .details("Team profile earned veteran recognition from "
                                + qualifyingCount + " distinct completed seasons")
                        .build());
                record(recognition, TimelineEventType.RECOGNITION_AWARDED, TimelineScope.EVENT_PARTICIPANTS,
                        "Hackathon Veteran earned", "The team earned veteran recognition", "awarded");
            }
            if (recognition.isActive()) result.put(profileId, summary(recognition));
        }
        return result;
    }

    @Transactional
    public RecognitionDtos.EvidenceResponse recalculate(UUID profileId, Authentication authentication) {
        TeamProfile profile = profile(profileId);
        User actor = actor(authentication);
        evaluateProfiles(List.of(profile.getId()), actor);
        return evidence(profileId);
    }

    @Transactional(readOnly = true)
    public RecognitionDtos.EvidenceResponse evidence(UUID profileId) {
        profile(profileId);
        List<EventTeamFinish> valid = finishes.findByTeamProfileIdIn(List.of(profileId)).stream()
                .filter(f -> f.getEvent().getStatus() == EventStatus.completed)
                .filter(f -> "completed".equals(f.getCompletionStatus()))
                .filter(f -> f.getTeam().getStatus() != TeamStatus.disqualified)
                .toList();
        Map<UUID, EventTeamFinish> onePerEvent = valid.stream()
                .collect(Collectors.toMap(f -> f.getEvent().getId(), Function.identity(),
                        (left, right) -> left.getFinalRank() <= right.getFinalRank() ? left : right,
                        LinkedHashMap::new));
        List<RecognitionDtos.QualifyingSeason> seasons = onePerEvent.values().stream()
                .sorted(Comparator.comparing(EventTeamFinish::getCompletedAt))
                .map(f -> new RecognitionDtos.QualifyingSeason(
                        f.getId(), f.getEvent().getId(), f.getEvent().getTitle(),
                        f.getTrack().getId(), f.getTrack().getName(), f.getFinalRank(),
                        f.getResultVersion().getId(), f.getCompletedAt()))
                .toList();
        TeamRecognition recognition = recognitions
                .findFirstByTeamProfileIdAndRecognitionCodeOrderByCreatedAtDesc(profileId, VETERAN_CODE)
                .orElse(null);
        return new RecognitionDtos.EvidenceResponse(
                profileId, recognition == null ? null : recognition.getId(),
                recognition == null ? null : summary(recognition), seasons.size(), seasons);
    }

    @Transactional
    public RecognitionDtos.EvidenceResponse revoke(UUID profileId, UUID recognitionId,
                                                   RecognitionDtos.RevokeRequest request,
                                                   Authentication authentication) {
        profile(profileId);
        TeamRecognition recognition = recognitions.findById(recognitionId)
                .orElseThrow(() -> ApiException.notFound("Recognition not found: " + recognitionId));
        if (!recognition.getTeamProfile().getId().equals(profileId)) {
            throw ApiException.badRequest("Recognition does not belong to team profile");
        }
        if (recognition.isActive()) {
            User actor = actor(authentication);
            recognition.setActive(false);
            recognition.setRevokedAt(LocalDateTime.now());
            recognition.setRevokedBy(actor);
            recognition.setRevokeReason(request.reason().trim());
            recognitions.save(recognition);
            audits.save(AuditLog.builder()
                    .user(actor)
                    .action(AuditAction.TEAM_RECOGNITION_REVOKED)
                    .targetType("team_recognition")
                    .targetId(recognition.getId())
                    .oldValue(VETERAN_CODE)
                    .details("Recognition revoked through coordinator correction workflow")
                    .build());
            record(recognition, TimelineEventType.RECOGNITION_REVOKED, TimelineScope.COORDINATOR_PRIVATE,
                    "Recognition revoked", "A recognition was revoked through coordinator correction", "revoked");
        }
        return evidence(profileId);
    }

    /**
     * Explicit coordinator correction path. The revoked row is retained intact;
     * restoration creates a new active recognition row subject to the same
     * profile/code uniqueness rule for active records.
     */
    @Transactional
    public RecognitionDtos.EvidenceResponse restore(UUID profileId, UUID recognitionId,
                                                    Authentication authentication) {
        profile(profileId);
        TeamRecognition revoked = recognitions.findById(recognitionId)
                .orElseThrow(() -> ApiException.notFound("Recognition not found: " + recognitionId));
        if (!revoked.getTeamProfile().getId().equals(profileId)) {
            throw ApiException.badRequest("Recognition does not belong to team profile");
        }
        if (revoked.isActive()) {
            throw ApiException.conflict("Recognition is already active");
        }
        long qualifyingCount = finishes.countDistinctQualifyingEvents(
                profileId, EventStatus.completed, TeamStatus.disqualified);
        if (qualifyingCount < REQUIRED_SEASONS) {
            throw ApiException.conflict("Team profile no longer has three qualifying completed seasons");
        }
        User actor = actor(authentication);
        TeamRecognition restored = TeamRecognition.builder()
                .teamProfile(revoked.getTeamProfile())
                .recognitionCode(VETERAN_CODE)
                .label(VETERAN_LABEL)
                .qualificationCount(Math.toIntExact(qualifyingCount))
                .earnedAt(LocalDateTime.now())
                .active(true)
                .build();
        try {
            restored = recognitions.saveAndFlush(restored);
        } catch (DataIntegrityViolationException ex) {
            if (!isActiveRecognitionConflict(ex)) throw ex;
            throw ApiException.conflict("An active recognition already exists for this profile");
        }
        audits.save(AuditLog.builder()
                .user(actor)
                .action(AuditAction.TEAM_RECOGNITION_RESTORED)
                .targetType("team_recognition")
                .targetId(restored.getId())
                .oldValue(recognitionId.toString())
                .newValue(VETERAN_CODE)
                .details("Recognition restored through explicit coordinator correction workflow")
                .build());
        record(restored, TimelineEventType.RECOGNITION_RESTORED, TimelineScope.EVENT_PARTICIPANTS,
                "Hackathon Veteran restored", "Veteran recognition was restored", "restored");
        return evidence(profileId);
    }

    @Transactional(readOnly = true)
    public Map<UUID, List<RecognitionDtos.Summary>> activeByTeamIds(Collection<UUID> teamIds) {
        if (teamIds == null || teamIds.isEmpty()) return Map.of();
        Map<UUID, List<RecognitionDtos.Summary>> result = new HashMap<>();
        for (Object[] row : recognitions.findActiveRowsByTeamIds(new LinkedHashSet<>(teamIds))) {
            UUID teamId = (UUID) row[0];
            TeamRecognition recognition = (TeamRecognition) row[1];
            result.computeIfAbsent(teamId, ignored -> new ArrayList<>()).add(summary(recognition));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<UUID, List<RecognitionDtos.Summary>> activeByProfileIds(Collection<UUID> profileIds) {
        if (profileIds == null || profileIds.isEmpty()) return Map.of();
        return recognitions.findByTeamProfileIdInAndActiveTrue(new LinkedHashSet<>(profileIds)).stream()
                .collect(Collectors.groupingBy(r -> r.getTeamProfile().getId(),
                        Collectors.mapping(this::summary, Collectors.toList())));
    }

    public RecognitionDtos.Summary summary(TeamRecognition recognition) {
        return new RecognitionDtos.Summary(
                recognition.getRecognitionCode(), recognition.getLabel(),
                recognition.getQualificationCount() + "+ Seasons",
                recognition.getQualificationCount(), recognition.getEarnedAt(),
                recognition.isActive());
    }

    private void record(TeamRecognition recognition, TimelineEventType type, TimelineScope scope,
                        String title, String description, String suffix) {
        if (timeline == null || teams == null) return;
        Team team = teams.findByTeamProfileIdIn(List.of(recognition.getTeamProfile().getId())).stream()
                .max(Comparator.comparing(Team::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);
        if (team == null) return;
        timeline.record(new TimelineEventRequest(team.getTrack().getEvent().getId(),
                scope == TimelineScope.COORDINATOR_PRIVATE ? null : team.getId(), null, team.getTrack().getId(),
                type, TimelineSourceType.RECOGNITION, recognition.getId(), scope, title, description, null,
                "recognition:" + recognition.getId() + ":" + suffix));
    }

    private boolean isActiveRecognitionConflict(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && message.contains("uq_team_recognition_active")) return true;
        }
        return false;
    }

    private TeamProfile profile(UUID profileId) {
        return profiles.findById(profileId)
                .orElseThrow(() -> ApiException.notFound("Team profile not found: " + profileId));
    }

    private User actor(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser current)) {
            throw ApiException.unauthorized("Authentication required");
        }
        return users.findById(current.getId())
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }
}
