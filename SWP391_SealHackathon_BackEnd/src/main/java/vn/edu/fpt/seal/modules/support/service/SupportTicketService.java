package vn.edu.fpt.seal.modules.support.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.enums.EventStatus;
import vn.edu.fpt.seal.common.enums.TeamStatus;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.support.dto.CreateSupportTicketRequest;
import vn.edu.fpt.seal.modules.support.dto.SupportTicketResponse;
import vn.edu.fpt.seal.modules.support.dto.UpdateSupportTicketStatusRequest;
import vn.edu.fpt.seal.modules.support.entity.SupportTicket;
import vn.edu.fpt.seal.modules.support.repository.SupportTicketRepository;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.team.repository.TeamMemberRepository;
import vn.edu.fpt.seal.modules.timeline.TimelineEventType;
import vn.edu.fpt.seal.modules.timeline.TimelineScope;
import vn.edu.fpt.seal.modules.timeline.TimelineSourceType;
import vn.edu.fpt.seal.modules.timeline.dto.TimelineEventRequest;
import vn.edu.fpt.seal.modules.timeline.service.TimelineService;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.security.AuthorizationService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupportTicketService {
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "open", Set.of("in_progress"),
            "in_progress", Set.of("open", "resolved"),
            "resolved", Set.of("open")
    );

    private final SupportTicketRepository repository;
    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;
    @Autowired
    private TimelineService timeline;
    @Autowired
    private TeamMemberRepository teamMembers;

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> list(UUID requesterId, Authentication authentication) {
        var user = authorizationService.current(authentication);
        if (authorizationService.isCoordinator(user)) {
            return (requesterId == null
                    ? repository.findTop100ByOrderByCreatedAtDesc()
                    : repository.findByRequesterIdOrderByCreatedAtDesc(requesterId))
                    .stream().map(this::map).toList();
        }
        if (requesterId != null && !requesterId.equals(user.getId())) {
            throw ApiException.forbidden("You can only list your own support tickets");
        }
        return repository.findByRequesterIdOrderByCreatedAtDesc(user.getId()).stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public SupportTicketResponse get(UUID id, Authentication authentication) {
        SupportTicket ticket = find(id);
        var user = authorizationService.current(authentication);
        authorizationService.require(
                authorizationService.isCoordinator(user) || ticket.getRequester().getId().equals(user.getId()),
                "You can only view your own support tickets");
        return map(ticket);
    }

    @Transactional
    public SupportTicketResponse create(CreateSupportTicketRequest request, UUID requesterId) {
        var requester = userRepository.findById(requesterId)
                .orElseThrow(() -> ApiException.notFound("Requester not found"));
        SupportTicket ticket = repository.save(SupportTicket.builder()
                .requester(requester)
                .category(request.category())
                .priority(request.priority())
                .subject(request.subject().trim())
                .description(request.description().trim())
                .build());
        record(ticket, TimelineEventType.SUPPORT_CREATED, "Support ticket created",
                "The team requested support", "created");
        return map(ticket);
    }

    @Transactional
    public SupportTicketResponse updateStatus(UUID id, UpdateSupportTicketStatusRequest request) {
        SupportTicket ticket = find(id);
        String current = ticket.getStatus();
        String target = request.status();
        if (current.equals(target)) {
            return map(ticket);
        }
        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
            throw ApiException.badRequest("Invalid support ticket status transition: " + current + " -> " + target);
        }
        ticket.setStatus(target);
        TimelineEventType type = switch (target) {
            case "in_progress" -> TimelineEventType.SUPPORT_IN_PROGRESS;
            case "resolved" -> TimelineEventType.SUPPORT_RESOLVED;
            case "open" -> TimelineEventType.SUPPORT_REOPENED;
            default -> null;
        };
        if (type != null) {
            String label = type == TimelineEventType.SUPPORT_IN_PROGRESS ? "Support ticket in progress"
                    : type == TimelineEventType.SUPPORT_RESOLVED ? "Support ticket resolved" : "Support ticket reopened";
            record(ticket, type, label, "The support request status changed", "status:" + target);
        }
        return map(ticket);
    }

    private SupportTicket find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Support ticket not found: " + id));
    }

    private SupportTicketResponse map(SupportTicket ticket) {
        var requester = ticket.getRequester();
        return new SupportTicketResponse(
                ticket.getId(), requester.getId(), requester.getFullName(), requester.getEmail(),
                ticket.getCategory(), ticket.getPriority(), ticket.getSubject(), ticket.getDescription(),
                ticket.getStatus(), ticket.getCreatedAt(), ticket.getUpdatedAt());
    }

    private void record(SupportTicket ticket, TimelineEventType type, String title, String description, String suffix) {
        if (timeline == null || teamMembers == null) return;
        Team team = teamMembers.findByUserIdOrderByJoinedAtDesc(ticket.getRequester().getId()).stream()
                .map(member -> member.getTeam())
                .filter(candidate -> candidate.getStatus() == TeamStatus.active)
                .filter(candidate -> Set.of(EventStatus.published, EventStatus.ongoing)
                        .contains(candidate.getTrack().getEvent().getStatus()))
                .findFirst().orElse(null);
        if (team == null) return;
        timeline.record(new TimelineEventRequest(team.getTrack().getEvent().getId(), team.getId(), null,
                team.getTrack().getId(), type, TimelineSourceType.SUPPORT_TICKET, ticket.getId(),
                TimelineScope.TEAM_PRIVATE, title, description, null,
                "support:" + ticket.getId() + ":" + suffix));
    }
}
