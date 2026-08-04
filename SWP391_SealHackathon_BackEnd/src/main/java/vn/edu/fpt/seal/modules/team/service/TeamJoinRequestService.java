package vn.edu.fpt.seal.modules.team.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.enums.AccountStatus;
import vn.edu.fpt.seal.common.enums.EventStatus;
import vn.edu.fpt.seal.common.enums.TeamMemberRole;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.team.dto.CreateJoinRequestRequest;
import vn.edu.fpt.seal.modules.team.dto.JoinRequestResponse;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.team.entity.TeamJoinRequest;
import vn.edu.fpt.seal.modules.team.entity.TeamMember;
import vn.edu.fpt.seal.modules.team.repository.TeamJoinRequestRepository;
import vn.edu.fpt.seal.modules.team.repository.TeamMemberRepository;
import vn.edu.fpt.seal.modules.team.repository.TeamRepository;
import vn.edu.fpt.seal.modules.user.entity.User;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.security.CurrentUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamJoinRequestService {

    private static final int MAX_TEAM_SIZE = 5;

    private final TeamJoinRequestRepository requestRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    /**
     * A student requests to join a team.
     */
    @Transactional
    public JoinRequestResponse create(CreateJoinRequestRequest req, Authentication auth) {
        UUID callerId = currentUserId(auth);
        Team team = teamRepository.findWithTrackById(req.teamId()).orElseThrow(() -> ApiException.notFound("Team not found: " + req.teamId()));
        ensureRegistrationOpen(team);
        User caller = userRepository.findById(callerId).orElseThrow(() -> ApiException.notFound("User not found"));
        if (caller.getStatus() != AccountStatus.approved)
            throw ApiException.badRequest("Only approved users can request to join teams");
        if (teamMemberRepository.existsByTeamIdAndUserId(team.getId(), callerId))
            throw ApiException.conflict("You are already a member of this team");
        if (teamMemberRepository.countByTeamId(team.getId()) >= MAX_TEAM_SIZE)
            throw ApiException.badRequest("This team is already full");
        if (requestRepository.existsByTeamIdAndUserIdAndStatus(team.getId(), callerId, "pending"))
            throw ApiException.conflict("You already have a pending request for this team");
        TeamJoinRequest saved = requestRepository.save(TeamJoinRequest.builder()
                .team(team).user(caller).status("pending")
                .message(req.message() == null ? null : req.message().trim())
                .build());
        log.info("Join request created: team={}, user={}", team.getId(), callerId);
        return toResponse(saved);
    }

    /**
     * Leader (or coordinator) lists requests for a team, optionally filtered by status.
     */
    @Transactional(readOnly = true)
    public List<JoinRequestResponse> listForTeam(UUID teamId, String status, Authentication auth) {
        ensureLeaderOrCoordinator(teamId, auth);
        List<TeamJoinRequest> list = (status == null || status.isBlank())
                ? requestRepository.findByTeamIdOrderByCreatedAtDesc(teamId)
                : requestRepository.findByTeamIdAndStatusOrderByCreatedAtDesc(teamId, status.trim().toLowerCase());
        return list.stream().map(TeamJoinRequestService::toResponse).toList();
    }

    /**
     * A student lists their own join requests.
     */
    @Transactional(readOnly = true)
    public List<JoinRequestResponse> listMine(Authentication auth) {
        return requestRepository.findByUserIdOrderByCreatedAtDesc(currentUserId(auth)).stream().map(TeamJoinRequestService::toResponse).toList();
    }

    /**
     * Leader accepts a pending request -> adds the user as a member.
     */
    @Transactional
    public JoinRequestResponse accept(UUID id, Authentication auth) {
        TeamJoinRequest r = requestRepository.findWithRelationsById(id).orElseThrow(() -> ApiException.notFound("Join request not found: " + id));
        ensureLeaderOrCoordinator(r.getTeam().getId(), auth);
        if (!"pending".equals(r.getStatus()))
            throw ApiException.badRequest("Request is not pending (current: " + r.getStatus() + ")");
        Team team = r.getTeam();
        ensureRegistrationOpen(team);
        if (teamMemberRepository.existsByTeamIdAndUserId(team.getId(), r.getUser().getId())) {
            // Already a member (e.g. joined via code meanwhile): just close the request.
            r.setStatus("accepted");
            r.setRespondedAt(LocalDateTime.now());
            return toResponse(r);
        }
        if (teamMemberRepository.countByTeamId(team.getId()) >= MAX_TEAM_SIZE)
            throw ApiException.badRequest("Team is already full");
        if (r.getUser().getStatus() != AccountStatus.approved)
            throw ApiException.badRequest("Only approved users can join teams");
        if (teamMemberRepository.existsActiveRegistrationInEvent(r.getUser().getId(), team.getTrack().getEvent().getId()))
            throw ApiException.conflict("User already belongs to another active team in this event");
        teamMemberRepository.save(TeamMember.builder().team(team).user(r.getUser()).role(TeamMemberRole.member).build());
        r.setStatus("accepted");
        r.setRespondedAt(LocalDateTime.now());
        log.info("Join request accepted: team={}, user={}", team.getId(), r.getUser().getId());
        return toResponse(r);
    }

    /**
     * Leader rejects a pending request.
     */
    @Transactional
    public JoinRequestResponse reject(UUID id, Authentication auth) {
        TeamJoinRequest r = requestRepository.findWithRelationsById(id).orElseThrow(() -> ApiException.notFound("Join request not found: " + id));
        ensureLeaderOrCoordinator(r.getTeam().getId(), auth);
        if (!"pending".equals(r.getStatus()))
            throw ApiException.badRequest("Request is not pending (current: " + r.getStatus() + ")");
        r.setStatus("rejected");
        r.setRespondedAt(LocalDateTime.now());
        return toResponse(r);
    }

    /**
     * The requesting student cancels their own pending request.
     */
    @Transactional
    public void cancel(UUID id, Authentication auth) {
        TeamJoinRequest r = requestRepository.findWithRelationsById(id).orElseThrow(() -> ApiException.notFound("Join request not found: " + id));
        if (!r.getUser().getId().equals(currentUserId(auth)))
            throw ApiException.forbidden("You can only cancel your own request");
        if (!"pending".equals(r.getStatus())) throw ApiException.badRequest("Request is not pending");
        r.setStatus("cancelled");
        r.setRespondedAt(LocalDateTime.now());
    }

    // --- helpers ---
    private void ensureRegistrationOpen(Team team) {
        EventStatus s = team.getTrack().getEvent().getStatus();
        if (s != EventStatus.published)
            throw ApiException.badRequest("Registration is not open for this event (status: " + s + ")");
    }

    private void ensureLeaderOrCoordinator(UUID teamId, Authentication auth) {
        if (isCoordinator(auth)) return;
        UUID callerId = currentUserId(auth);
        TeamMember m = teamMemberRepository.findByTeamIdAndUserId(teamId, callerId)
                .orElseThrow(() -> ApiException.forbidden("Only the team leader can manage join requests"));
        if (m.getRole() != TeamMemberRole.leader)
            throw ApiException.forbidden("Only the team leader can manage join requests");
    }

    private boolean isCoordinator(Authentication auth) {
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_COORDINATOR"));
    }

    private UUID currentUserId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof CurrentUser c) return c.getId();
        throw ApiException.forbidden("Authentication required");
    }

    private static JoinRequestResponse toResponse(TeamJoinRequest r) {
        return JoinRequestResponse.builder()
                .id(r.getId())
                .teamId(r.getTeam().getId())
                .teamName(r.getTeam().getName())
                .userId(r.getUser().getId())
                .userEmail(r.getUser().getEmail())
                .userFullName(r.getUser().getFullName())
                .status(r.getStatus())
                .message(r.getMessage())
                .createdAt(r.getCreatedAt())
                .respondedAt(r.getRespondedAt())
                .build();
    }
}
