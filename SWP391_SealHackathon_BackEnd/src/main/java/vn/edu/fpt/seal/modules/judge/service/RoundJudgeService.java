package vn.edu.fpt.seal.modules.judge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.enums.AccountStatus;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.judge.dto.AssignRoundJudgeRequest;
import vn.edu.fpt.seal.modules.judge.dto.JudgeSubmissionResponse;
import vn.edu.fpt.seal.modules.judge.dto.RoundJudgeResponse;
import vn.edu.fpt.seal.modules.judge.entity.RoundJudge;
import vn.edu.fpt.seal.modules.judge.mapper.RoundJudgeMapper;
import vn.edu.fpt.seal.modules.judge.repository.RoundJudgeRepository;
import vn.edu.fpt.seal.modules.recognition.service.TeamRecognitionService;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;
import vn.edu.fpt.seal.modules.user.entity.User;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.security.AuthorizationService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class RoundJudgeService {
    private final RoundJudgeRepository roundJudgeRepository;
    private final RoundRepository roundRepository;
    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;
    private final TeamRecognitionService recognitionService;

    public RoundJudgeService(RoundJudgeRepository assignments, RoundRepository rounds,
                             UserRepository users, AuthorizationService authorization) {
        this.roundJudgeRepository = assignments;
        this.roundRepository = rounds;
        this.userRepository = users;
        this.authorizationService = authorization;
        this.recognitionService = null;
    }

    @Transactional(readOnly = true)
    public Page<RoundJudgeResponse> list(UUID eventId, UUID roundId, UUID trackId, UUID userId, Pageable pageable, Authentication authentication) {
        var current = authorizationService.current(authentication);
        Round selectedRound = validateHierarchy(eventId, roundId, trackId);
        if (!authorizationService.isCoordinator(current)) {
            authorizationService.require(authorizationService.hasRole(current, "judge"),
                    "Only judges and coordinators can view judge assignments");
            if (userId != null && !userId.equals(current.getId())) {
                throw ApiException.forbidden("Judges can only view their own assignments");
            }
            if (roundId != null) {
                RoundJudge assignment = roundJudgeRepository.findByRoundIdAndUserId(roundId, current.getId())
                        .orElseThrow(() -> ApiException.forbidden("Judge is not assigned to this round"));
                return new PageImpl<>(List.of(RoundJudgeMapper.toResponse(assignment)), pageable, 1);
            }
            return eventId == null ? roundJudgeRepository.findByUserId(current.getId(), pageable).map(RoundJudgeMapper::toResponse)
                    : roundJudgeRepository.findByRoundTrackEventIdAndUserId(eventId, current.getId(), pageable).map(RoundJudgeMapper::toResponse);
        }
        if (eventId == null) throw ApiException.badRequest("eventId is required for coordinator judge queries");
        if (selectedRound != null)
            return roundJudgeRepository.findByRoundTrackEventIdAndRoundId(eventId, roundId, pageable).map(RoundJudgeMapper::toResponse);
        if (userId != null)
            return roundJudgeRepository.findByRoundTrackEventIdAndUserId(eventId, userId, pageable).map(RoundJudgeMapper::toResponse);
        return roundJudgeRepository.findByRoundTrackEventId(eventId, pageable).map(RoundJudgeMapper::toResponse);
    }

    @Transactional
    public RoundJudgeResponse assign(AssignRoundJudgeRequest req) {
        Round round = roundRepository.findById(req.roundId()).orElseThrow(() -> ApiException.notFound("Round not found: " + req.roundId()));
        User user = userRepository.findById(req.userId()).orElseThrow(() -> ApiException.notFound("User not found: " + req.userId()));
        if (user.getStatus() != AccountStatus.approved)
            throw ApiException.badRequest("Only approved users can be assigned as judges");
        boolean hasJudgeRole = user.getRoles() != null && user.getRoles().stream().anyMatch(r -> "judge".equalsIgnoreCase(r.getName()));
        if (!hasJudgeRole) throw ApiException.badRequest("Assigned user must have judge role");
        if (roundJudgeRepository.existsByRoundIdAndUserId(round.getId(), user.getId()))
            throw ApiException.conflict("Judge already assigned to this round");
        return RoundJudgeMapper.toResponse(roundJudgeRepository.save(RoundJudge.builder().round(round).user(user).build()));
    }

    @Transactional
    public void remove(UUID id) {
        roundJudgeRepository.delete(roundJudgeRepository.findById(id).orElseThrow(() -> ApiException.notFound("Round judge assignment not found: " + id)));
    }

    @Transactional
    public void removeByRoundAndUser(UUID roundId, UUID userId) {
        roundJudgeRepository.delete(roundJudgeRepository.findByRoundIdAndUserId(roundId, userId).orElseThrow(() -> ApiException.notFound("Round judge assignment not found")));
    }

    @Transactional(readOnly = true)
    public List<JudgeSubmissionResponse> submissions(UUID judgeId, UUID eventId, UUID roundId, UUID trackId, Authentication authentication) {
        var current = authorizationService.current(authentication);
        if (!authorizationService.isCoordinator(current) && !judgeId.equals(current.getId())) {
            throw ApiException.forbidden("Judges can only view their own assigned submissions");
        }
        if (!authorizationService.isCoordinator(current)) {
            authorizationService.require(authorizationService.hasRole(current, "judge"),
                    "Judge role is required");
        }
        validateHierarchy(eventId, roundId, trackId);
        var rows = roundJudgeRepository.findSubmissionRowsForJudge(judgeId, eventId, roundId, trackId);
        var recognitionByTeam = recognitionService == null
                ? Map.<UUID, List<vn.edu.fpt.seal.modules.recognition.dto.RecognitionDtos.Summary>>of()
                : recognitionService.activeByTeamIds(
                rows.stream().map(RoundJudgeRepository.JudgeSubmissionRow::getTeamId).toList());
        return rows.stream().map(row -> JudgeSubmissionResponse.builder()
                .roundJudgeId(row.getRoundJudgeId()).judgeId(row.getJudgeId()).eventId(row.getEventId()).eventName(row.getEventName())
                .trackId(row.getTrackId()).trackName(row.getTrackName()).roundId(row.getRoundId()).roundName(row.getRoundName())
                .teamId(row.getTeamId()).teamName(row.getTeamName()).submissionId(row.getSubmissionId()).repoUrl(row.getRepoUrl())
                .presentationUrl(row.getPresentationUrl()).demoUrl(row.getDemoUrl()).status(row.getStatus())
                .reviewStatus(row.getReviewStatus()).submittedAt(row.getSubmittedAt())
                .recognitions(recognitionByTeam.getOrDefault(row.getTeamId(), List.of())).build()).toList();
    }

    private Round validateHierarchy(UUID eventId, UUID roundId, UUID trackId) {
        if (roundId == null) {
            if (trackId != null) throw ApiException.badRequest("roundId is required when trackId is supplied");
            return null;
        }
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> ApiException.notFound("Round not found: " + roundId));
        if ((eventId != null && !round.getTrack().getEvent().getId().equals(eventId))
                || (trackId != null && !round.getTrack().getId().equals(trackId))) {
            throw ApiException.badRequest("Judge assignment hierarchy does not match event, round, and track");
        }
        return round;
    }

    public UUID currentJudgeId(Authentication authentication) {
        var current = authorizationService.current(authentication);
        authorizationService.require(authorizationService.hasRole(current, "judge"), "Judge role is required");
        return current.getId();
    }
}
