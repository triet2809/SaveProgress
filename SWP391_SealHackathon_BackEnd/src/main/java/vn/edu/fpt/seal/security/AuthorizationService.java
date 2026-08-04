package vn.edu.fpt.seal.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.incident.entity.IncidentReport;
import vn.edu.fpt.seal.modules.judge.repository.RoundJudgeRepository;
import vn.edu.fpt.seal.modules.mentor.repository.TrackMentorRepository;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.submission.entity.Submission;
import vn.edu.fpt.seal.modules.team.repository.TeamMemberRepository;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthorizationService {
    private final TeamMemberRepository teamMemberRepository;
    private final RoundJudgeRepository roundJudgeRepository;
    private final TrackMentorRepository trackMentorRepository;

    public CurrentUser current(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser user)) {
            throw ApiException.forbidden("Authentication required");
        }
        return user;
    }

    public boolean hasRole(CurrentUser user, String role) {
        return user.getRoles() != null
                && user.getRoles().stream().anyMatch(value -> role.equalsIgnoreCase(value));
    }

    public boolean isCoordinator(CurrentUser user) {
        return hasRole(user, "coordinator");
    }

    public boolean isTeamMember(CurrentUser user, UUID teamId) {
        return teamId != null && teamMemberRepository.existsByTeamIdAndUserId(teamId, user.getId());
    }

    public boolean isAssignedJudge(CurrentUser user, UUID roundId) {
        return roundId != null && roundJudgeRepository.existsByRoundIdAndUserId(roundId, user.getId());
    }

    public boolean isAssignedMentor(CurrentUser user, UUID trackId) {
        return trackId != null && trackMentorRepository.existsByTrackIdAndUserId(trackId, user.getId());
    }

    public boolean canReadSubmission(CurrentUser user, Submission submission) {
        return isCoordinator(user)
                || isTeamMember(user, submission.getTeam().getId())
                || isAssignedJudge(user, submission.getRound().getId())
                || isAssignedMentor(user, submission.getTeam().getTrack().getId());
    }

    public boolean canReadDetailedScore(CurrentUser user, Submission submission) {
        if (isCoordinator(user)
                || isAssignedJudge(user, submission.getRound().getId())
                || isAssignedMentor(user, submission.getTeam().getTrack().getId())) {
            return true;
        }
        return submission.getRound().getResultPublishedAt() != null
                && isTeamMember(user, submission.getTeam().getId());
    }

    public boolean canReadRanking(CurrentUser user, Round round) {
        if (isCoordinator(user)
                || isAssignedJudge(user, round.getId())
                || isAssignedMentor(user, round.getTrack().getId())) {
            return true;
        }
        return round.getResultPublishedAt() != null
                && teamMemberRepository.existsByUserIdAndTeamTrackId(user.getId(), round.getTrack().getId());
    }

    public boolean canReadIncident(CurrentUser user, IncidentReport incident) {
        if (isCoordinator(user) || incident.getReporter().getId().equals(user.getId())) {
            return true;
        }
        if (incident.getTeam() != null && isTeamMember(user, incident.getTeam().getId())) {
            return true;
        }
        UUID roundId = incident.getRound() != null
                ? incident.getRound().getId()
                : incident.getSubmission() != null ? incident.getSubmission().getRound().getId() : null;
        if (isAssignedJudge(user, roundId)) {
            return true;
        }
        UUID trackId = incident.getTrack() != null
                ? incident.getTrack().getId()
                : incident.getTeam() != null
                ? incident.getTeam().getTrack().getId()
                : incident.getRound() != null
                ? incident.getRound().getTrack().getId()
                : incident.getSubmission() != null
                ? incident.getSubmission().getTeam().getTrack().getId()
                : null;
        return isAssignedMentor(user, trackId);
    }

    public void require(boolean allowed, String message) {
        if (!allowed) {
            throw ApiException.forbidden(message);
        }
    }
}
