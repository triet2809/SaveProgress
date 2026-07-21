package vn.edu.fpt.seal.modules.staff.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.enums.*;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.event.repository.EventRepository;
import vn.edu.fpt.seal.modules.judge.entity.*;
import vn.edu.fpt.seal.modules.judge.repository.*;
import vn.edu.fpt.seal.modules.mentor.entity.TrackMentor;
import vn.edu.fpt.seal.modules.mentor.repository.TrackMentorRepository;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;
import vn.edu.fpt.seal.modules.staff.dto.*;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.track.repository.TrackRepository;
import vn.edu.fpt.seal.modules.user.entity.*;
import vn.edu.fpt.seal.modules.user.repository.*;
import vn.edu.fpt.seal.modules.auth.service.AccountActivationService;

import java.util.*;

@Service
public class EventStaffService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TrackRepository trackRepository;
    private final RoundRepository roundRepository;
    private final TrackMentorRepository mentorRepository;
    private final TrackJudgeRepository trackJudgeRepository;
    private final RoundJudgeRepository roundJudgeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountActivationService activationService;
    private final vn.edu.fpt.seal.config.AppProperties appProperties;

    @Autowired
    public EventStaffService(EventRepository eventRepository, UserRepository userRepository, RoleRepository roleRepository,
                             TrackRepository trackRepository, RoundRepository roundRepository, TrackMentorRepository mentorRepository,
                             TrackJudgeRepository trackJudgeRepository, RoundJudgeRepository roundJudgeRepository, PasswordEncoder passwordEncoder,
                             AccountActivationService activationService, vn.edu.fpt.seal.config.AppProperties appProperties) {
        this.eventRepository = eventRepository; this.userRepository = userRepository; this.roleRepository = roleRepository;
        this.trackRepository = trackRepository; this.roundRepository = roundRepository; this.mentorRepository = mentorRepository;
        this.trackJudgeRepository = trackJudgeRepository; this.roundJudgeRepository = roundJudgeRepository; this.passwordEncoder = passwordEncoder;
        this.activationService = activationService; this.appProperties = appProperties;
    }

    public EventStaffService(EventRepository e, UserRepository u, RoleRepository r, TrackRepository t, RoundRepository rd,
                             TrackMentorRepository m, TrackJudgeRepository tj, RoundJudgeRepository rj, PasswordEncoder p) {
        this(e, u, r, t, rd, m, tj, rj, p, null, new vn.edu.fpt.seal.config.AppProperties());
    }

    @Transactional(readOnly = true)
    public List<EventStaffResponse> list(UUID eventId) {
        requireEvent(eventId);
        Map<UUID, StaffAccumulator> staff = new LinkedHashMap<>();
        mentorRepository.findAllByEventId(eventId).forEach(a -> staff.computeIfAbsent(a.getUser().getId(), k -> new StaffAccumulator(a.getUser()))
                .mentor(a.getTrack()));
        trackJudgeRepository.findAllByEventId(eventId).forEach(a -> staff.computeIfAbsent(a.getUser().getId(), k -> new StaffAccumulator(a.getUser()))
                .judgeTrack(a.getTrack()));
        roundJudgeRepository.findAllByRoundTrackEventId(eventId).forEach(a -> staff.computeIfAbsent(a.getUser().getId(), k -> new StaffAccumulator(a.getUser()))
                .judgeRound(a.getRound()));
        return staff.values().stream().map(StaffAccumulator::build).toList();
    }

    @Transactional
    public EventStaffResponse invite(UUID eventId, InviteStaffRequest request) {
        Event event = requireEvent(eventId);
        Set<String> requested = normalizeRoles(request.roles());
        Optional<User> existing = userRepository.findByEmail(request.email().trim().toLowerCase());
        if (existing.isPresent() && existing.get().getStatus() == AccountStatus.rejected) {
            throw ApiException.conflict("Rejected accounts must be reinstated before staff assignment");
        }
        if (existing.isPresent() && request.temporaryPassword() != null && !request.temporaryPassword().isBlank()) {
            throw ApiException.conflict("Email already registered; assign the existing account without a temporary password");
        }
        User user = existing.orElseGet(() -> createApprovedWithTemporaryPassword(request));
        if (request.fullName() != null && !request.fullName().isBlank() && user.getFullName() == null) user.setFullName(request.fullName().trim());
        ensureGlobalRoles(user, requested);
        userRepository.save(user);
        applyAssignments(event, user, requested, request.mentorTrackIds(), request.judgeTrackIds(), request.judgeRoundIds());
        EventStaffResponse base = list(eventId).stream().filter(s -> s.userId().equals(user.getId())).findFirst()
                .orElseThrow(() -> ApiException.badRequest("At least one event assignment is required"));
        if (user.getStatus() == AccountStatus.approved) return base;
        if (activationService == null) return base;
        AccountActivationService.IssuedToken issued = activationService.issue(user);
        String url = appProperties.getInvitation().isExposeLink()
                ? appProperties.getInvitation().getFrontendBaseUrl() + "/activate-account?token=" + issued.raw() : null;
        return new EventStaffResponse(base.userId(), base.fullName(), base.email(), base.accountStatus(), base.globalRoles(), base.eventRoles(),
                base.mentorTrackIds(), base.mentorTracks(), base.judgeTrackIds(), base.judgeTracks(), base.judgeRoundIds(), base.judgeRounds(),
                base.createdAt(), appProperties.getInvitation().isExposeLink() ? "development_link" : "not_configured", url);
    }

    @Transactional
    public EventStaffResponse update(UUID eventId, UUID userId, UpdateStaffAssignmentsRequest request) {
        Event event = requireEvent(eventId);
        User user = userRepository.findWithRolesById(userId).orElseThrow(() -> ApiException.notFound("User not found: " + userId));
        Set<String> roles = new HashSet<>();
        if (!request.mentorTrackIds().isEmpty()) roles.add("mentor");
        if (!request.judgeTrackIds().isEmpty() || !request.judgeRoundIds().isEmpty()) roles.add("judge");
        ensureGlobalRoles(user, roles);
        replaceMentorAssignments(event, user, request.mentorTrackIds());
        replaceJudgeAssignments(event, user, request.judgeTrackIds(), request.judgeRoundIds());
        return list(eventId).stream().filter(s -> s.userId().equals(userId)).findFirst()
                .orElseThrow(() -> ApiException.notFound("No assignments remain for this user in the event"));
    }

    @Transactional
    public void remove(UUID eventId, UUID userId, String assignmentType) {
        Event event = requireEvent(eventId);
        String type = assignmentType.toLowerCase(Locale.ROOT);
        if ("mentor".equals(type)) mentorRepository.deleteAll(mentorRepository.findAllByEventId(eventId).stream().filter(a -> a.getUser().getId().equals(userId)).toList());
        else if ("judge".equals(type)) {
            trackJudgeRepository.deleteAll(trackJudgeRepository.findAllByEventId(eventId).stream().filter(a -> a.getUser().getId().equals(userId)).toList());
            roundJudgeRepository.deleteAll(roundJudgeRepository.findAllByRoundTrackEventId(eventId).stream().filter(a -> a.getUser().getId().equals(userId)).toList());
        } else throw ApiException.badRequest("assignmentType must be mentor or judge");
        if (list(eventId).stream().noneMatch(s -> s.userId().equals(userId))) {
            // Deliberately preserve global roles and assignments in other events.
        }
    }

    private Event requireEvent(UUID id) {
        return eventRepository.findById(id).orElseThrow(() -> ApiException.notFound("Event not found: " + id));
    }

    private User createApprovedWithTemporaryPassword(InviteStaffRequest r) {
        validateTemporaryPassword(r.temporaryPassword());
        return userRepository.save(User.builder().email(r.email().trim().toLowerCase()).fullName(r.fullName().trim())
                .passwordHash(passwordEncoder.encode(r.temporaryPassword()))
                .status(AccountStatus.approved).mustChangePassword(true)
                .studentType(StudentType.none).isGuest(false).roles(new HashSet<>()).build());
    }

    private void validateTemporaryPassword(String password) {
        if (password == null || !password.matches("^(?=.*[A-Za-z])(?=.*\\d).{8,72}$")) {
            throw ApiException.badRequest("Temporary password must be 8-72 characters and contain letters and numbers");
        }
    }

    private Set<String> normalizeRoles(Collection<String> roles) {
        Set<String> result = new HashSet<>();
        for (String role : roles) {
            String normalized = role.toLowerCase(Locale.ROOT).replace("role_", "");
            if (normalized.equals("judge") || normalized.equals("mentor")) result.add(normalized);
            else throw ApiException.badRequest("Only judge and mentor assignments are supported");
        }
        if (result.isEmpty()) throw ApiException.badRequest("At least one staff role is required");
        return result;
    }

    private void ensureGlobalRoles(User user, Set<String> roles) {
        for (String roleName : roles) {
            Role role = roleRepository.findByName(roleName).orElseThrow(() -> ApiException.notFound("Role not found: " + roleName));
            user.getRoles().add(role);
        }
    }

    private void applyAssignments(Event event, User user, Set<String> roles, Set<UUID> mentorTracks, Set<UUID> judgeTracks, Set<UUID> judgeRounds) {
        if (roles.contains("mentor")) replaceMentorAssignments(event, user, mentorTracks == null ? Set.of() : mentorTracks);
        if (roles.contains("judge")) replaceJudgeAssignments(event, user, judgeTracks == null ? Set.of() : judgeTracks, judgeRounds == null ? Set.of() : judgeRounds);
        if (roles.contains("mentor") && (mentorTracks == null || mentorTracks.isEmpty())) throw ApiException.badRequest("Mentor role requires at least one track");
        if (roles.contains("judge") && (judgeTracks == null || judgeTracks.isEmpty()) && (judgeRounds == null || judgeRounds.isEmpty())) throw ApiException.badRequest("Judge role requires at least one track or round");
    }

    private void replaceMentorAssignments(Event event, User user, Set<UUID> ids) {
        List<TrackMentor> existing = mentorRepository.findAllByEventId(event.getId()).stream().filter(a -> a.getUser().getId().equals(user.getId())).toList();
        mentorRepository.deleteAll(existing);
        for (UUID id : ids) {
            Track track = trackRepository.findById(id).orElseThrow(() -> ApiException.notFound("Track not found: " + id));
            if (!track.getEvent().getId().equals(event.getId())) throw ApiException.badRequest("Mentor track belongs to another event");
            mentorRepository.save(TrackMentor.builder().event(event).track(track).user(user).build());
        }
    }

    private void replaceJudgeAssignments(Event event, User user, Set<UUID> trackIds, Set<UUID> roundIds) {
        trackJudgeRepository.deleteAll(trackJudgeRepository.findAllByEventId(event.getId()).stream().filter(a -> a.getUser().getId().equals(user.getId())).toList());
        roundJudgeRepository.deleteAll(roundJudgeRepository.findAllByRoundTrackEventId(event.getId()).stream().filter(a -> a.getUser().getId().equals(user.getId())).toList());
        for (UUID id : trackIds) {
            Track track = trackRepository.findById(id).orElseThrow(() -> ApiException.notFound("Track not found: " + id));
            if (!track.getEvent().getId().equals(event.getId())) throw ApiException.badRequest("Judge track belongs to another event");
            trackJudgeRepository.save(TrackJudge.builder().event(event).track(track).user(user).build());
        }
        for (UUID id : roundIds) {
            Round round = roundRepository.findById(id).orElseThrow(() -> ApiException.notFound("Round not found: " + id));
            if (!round.getTrack().getEvent().getId().equals(event.getId())) throw ApiException.badRequest("Judge round belongs to another event");
            roundJudgeRepository.save(RoundJudge.builder().round(round).user(user).build());
        }
    }

    private static final class StaffAccumulator {
        private final User user; private final Set<UUID> mentorTrackIds = new LinkedHashSet<>(), judgeTrackIds = new LinkedHashSet<>(), judgeRoundIds = new LinkedHashSet<>();
        private final List<String> mentorTracks = new ArrayList<>(), judgeTracks = new ArrayList<>(), judgeRounds = new ArrayList<>();
        private StaffAccumulator(User user) { this.user = user; }
        StaffAccumulator mentor(Track t) { mentorTrackIds.add(t.getId()); mentorTracks.add(t.getName()); return this; }
        StaffAccumulator judgeTrack(Track t) { judgeTrackIds.add(t.getId()); judgeTracks.add(t.getName()); return this; }
        StaffAccumulator judgeRound(Round r) { judgeRoundIds.add(r.getId()); judgeRounds.add(r.getName()); return this; }
        EventStaffResponse build() {
            List<String> globals = user.getRoles().stream().map(Role::getName).sorted().toList();
            Set<String> event = new LinkedHashSet<>(); if (!mentorTrackIds.isEmpty()) event.add("mentor"); if (!judgeTrackIds.isEmpty() || !judgeRoundIds.isEmpty()) event.add("judge");
            return EventStaffResponse.builder().userId(user.getId()).fullName(user.getFullName()).email(user.getEmail()).accountStatus(user.getStatus())
                    .globalRoles(globals).eventRoles(event.stream().toList()).mentorTrackIds(mentorTrackIds.stream().toList()).mentorTracks(mentorTracks)
                    .judgeTrackIds(judgeTrackIds.stream().toList()).judgeTracks(judgeTracks).judgeRoundIds(judgeRoundIds.stream().toList())
                    .judgeRounds(judgeRounds).createdAt(user.getCreatedAt()).activationDelivery("not_configured").activationUrl(null).build();
        }
    }
}
