package vn.edu.fpt.seal.modules.mentor.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import vn.edu.fpt.seal.common.enums.AccountStatus;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.mentor.dto.*;
import vn.edu.fpt.seal.modules.mentor.entity.TrackMentor;
import vn.edu.fpt.seal.modules.mentor.mapper.TrackMentorMapper;
import vn.edu.fpt.seal.modules.mentor.repository.TrackMentorRepository;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.track.repository.TrackRepository;
import vn.edu.fpt.seal.modules.user.entity.User;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;
import vn.edu.fpt.seal.modules.recognition.service.TeamRecognitionService;
import vn.edu.fpt.seal.security.AuthorizationService;

import java.util.*;

@Service @RequiredArgsConstructor(onConstructor_ = @Autowired)
public class TrackMentorService {
    private final TrackMentorRepository trackMentorRepository;
    private final TrackRepository trackRepository;
    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;
    private final RoundRepository roundRepository;
    private final TeamRecognitionService recognitionService;

    public TrackMentorService(TrackMentorRepository assignments, TrackRepository tracks,
                              UserRepository users, AuthorizationService authorization,
                              RoundRepository rounds) {
        this.trackMentorRepository = assignments;
        this.trackRepository = tracks;
        this.userRepository = users;
        this.authorizationService = authorization;
        this.roundRepository = rounds;
        this.recognitionService = null;
    }

    @Transactional(readOnly = true)
    public Page<TrackMentorResponse> list(UUID eventId, UUID trackId, UUID userId, Pageable pageable, Authentication authentication) {
        var current = authorizationService.current(authentication);
        validateTrackEvent(eventId, trackId);
        if (!authorizationService.isCoordinator(current)) {
            authorizationService.require(authorizationService.hasRole(current, "mentor"),
                    "Only mentors and coordinators can view mentor assignments");
            if (userId != null && !userId.equals(current.getId())) {
                throw ApiException.forbidden("Mentors can only view their own assignments");
            }
            if (trackId != null) {
                TrackMentor assignment = trackMentorRepository.findByTrackIdAndUserId(trackId, current.getId())
                        .orElseThrow(() -> ApiException.forbidden("Mentor is not assigned to this track"));
                return new PageImpl<>(List.of(TrackMentorMapper.toResponse(assignment)), pageable, 1);
            }
            return eventId == null ? trackMentorRepository.findByUserId(current.getId(), pageable).map(TrackMentorMapper::toResponse)
                    : trackMentorRepository.findByEventIdAndUserId(eventId, current.getId(), pageable).map(TrackMentorMapper::toResponse);
        }
        if (eventId == null) throw ApiException.badRequest("eventId is required for coordinator mentor queries");
        if (trackId != null) return trackMentorRepository.findByEventIdAndTrackId(eventId, trackId, pageable).map(TrackMentorMapper::toResponse);
        if (userId != null) return trackMentorRepository.findByEventIdAndUserId(eventId, userId, pageable).map(TrackMentorMapper::toResponse);
        return trackMentorRepository.findByEventId(eventId, pageable).map(TrackMentorMapper::toResponse);
    }

    @Transactional
    public TrackMentorResponse assign(AssignTrackMentorRequest req) {
        Track track = trackRepository.findById(req.trackId()).orElseThrow(() -> ApiException.notFound("Track not found: " + req.trackId()));
        User user = userRepository.findById(req.userId()).orElseThrow(() -> ApiException.notFound("User not found: " + req.userId()));
        if (user.getStatus() != AccountStatus.approved) throw ApiException.badRequest("Only approved users can be assigned as mentors");
        boolean hasMentorRole = user.getRoles() != null && user.getRoles().stream().anyMatch(r -> "mentor".equalsIgnoreCase(r.getName()));
        if (!hasMentorRole) throw ApiException.badRequest("Assigned user must have mentor role");
        if (trackMentorRepository.existsByTrackIdAndUserId(track.getId(), user.getId())) throw ApiException.conflict("Mentor already assigned to this track");
        return TrackMentorMapper.toResponse(trackMentorRepository.save(TrackMentor.builder().event(track.getEvent()).track(track).user(user).build()));
    }

    @Transactional public void remove(UUID id) { trackMentorRepository.delete(trackMentorRepository.findById(id).orElseThrow(() -> ApiException.notFound("Track mentor assignment not found: " + id))); }
    @Transactional public void removeByTrackAndUser(UUID trackId, UUID userId) { trackMentorRepository.delete(trackMentorRepository.findByTrackIdAndUserId(trackId, userId).orElseThrow(() -> ApiException.notFound("Track mentor assignment not found"))); }

    @Transactional(readOnly = true)
    public List<MentorTeamResponse> teams(UUID mentorId, UUID eventId, UUID trackId, UUID roundId, Authentication authentication) {
        var current = authorizationService.current(authentication);
        if (!authorizationService.isCoordinator(current) && !mentorId.equals(current.getId())) {
            throw ApiException.forbidden("Mentors can only view their own assigned teams");
        }
        if (!authorizationService.isCoordinator(current)) {
            authorizationService.require(authorizationService.hasRole(current, "mentor"),
                    "Mentor role is required");
        }
        validateTrackEvent(eventId, trackId);
        if (roundId != null) {
            var round = roundRepository.findById(roundId)
                    .orElseThrow(() -> ApiException.notFound("Round not found: " + roundId));
            if ((eventId != null && !round.getTrack().getEvent().getId().equals(eventId))
                    || (trackId != null && !round.getTrack().getId().equals(trackId))) {
                throw ApiException.badRequest("Round does not belong to the selected event and track");
            }
        }
        var rows = trackMentorRepository.findTeamRowsForMentor(mentorId, eventId, trackId, roundId);
        var recognitionByTeam = recognitionService == null
                ? Map.<UUID, List<vn.edu.fpt.seal.modules.recognition.dto.RecognitionDtos.Summary>>of()
                : recognitionService.activeByTeamIds(
                        rows.stream().map(TrackMentorRepository.MentorTeamRow::getTeamId).toList());
        return rows.stream().map(row -> MentorTeamResponse.builder()
                .trackMentorId(row.getTrackMentorId()).mentorId(row.getMentorId()).eventId(row.getEventId()).eventName(row.getEventName())
                .trackId(row.getTrackId()).trackName(row.getTrackName())
                .roundId(row.getRoundId()).roundName(row.getRoundName()).teamId(row.getTeamId()).teamName(row.getTeamName()).teamStatus(row.getTeamStatus())
                .recognitions(recognitionByTeam.getOrDefault(row.getTeamId(), List.of())).build()).toList();
    }

    private void validateTrackEvent(UUID eventId, UUID trackId) {
        if (trackId != null && (eventId == null || trackRepository.findById(trackId)
                .filter(t -> t.getEvent().getId().equals(eventId)).isEmpty())) {
            throw ApiException.badRequest("Track does not belong to the selected event");
        }
    }

    public UUID currentMentorId(Authentication authentication) {
        var current = authorizationService.current(authentication);
        authorizationService.require(authorizationService.hasRole(current, "mentor"), "Mentor role is required");
        return current.getId();
    }
}
