package vn.edu.fpt.seal.modules.feedback.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.feedback.dto.CreateMentorFeedbackRequest;
import vn.edu.fpt.seal.modules.feedback.dto.MentorFeedbackResponse;
import vn.edu.fpt.seal.modules.feedback.dto.UpdateMentorFeedbackRequest;
import vn.edu.fpt.seal.modules.feedback.entity.MentorFeedback;
import vn.edu.fpt.seal.modules.feedback.mapper.MentorFeedbackMapper;
import vn.edu.fpt.seal.modules.feedback.repository.MentorFeedbackRepository;
import vn.edu.fpt.seal.modules.mentor.entity.TrackMentor;
import vn.edu.fpt.seal.modules.mentor.repository.TrackMentorRepository;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.team.repository.TeamRepository;
import vn.edu.fpt.seal.security.CurrentUser;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MentorFeedbackService {
    private final MentorFeedbackRepository repo;
    private final TrackMentorRepository tmRepo;
    private final TeamRepository teamRepo;
    private final RoundRepository roundRepo;

    @Transactional(readOnly = true)
    public Page<MentorFeedbackResponse> list(UUID trackMentorId, UUID teamId, UUID roundId, Pageable p) {
        Page<MentorFeedback> page = trackMentorId != null ? repo.findByTrackMentorId(trackMentorId, p) : teamId != null ? repo.findByTeamId(teamId, p) : roundId != null ? repo.findByRoundId(roundId, p) : repo.findAll(p);
        return page.map(MentorFeedbackMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public MentorFeedbackResponse get(UUID id) {
        return MentorFeedbackMapper.toResponse(find(id));
    }

    @Transactional
    public MentorFeedbackResponse create(CreateMentorFeedbackRequest r, Authentication auth) {
        TrackMentor tm = tmRepo.findById(r.trackMentorId()).orElseThrow(() -> ApiException.notFound("Track mentor not found: " + r.trackMentorId()));
        authorizeMentorOrCoordinator(tm, auth);
        Team team = teamRepo.findById(r.teamId()).orElseThrow(() -> ApiException.notFound("Team not found: " + r.teamId()));
        if (!team.getTrack().getId().equals(tm.getTrack().getId()))
            throw ApiException.badRequest("Team must belong to mentor track");
        Round round = null;
        if (r.roundId() != null) {
            round = roundRepo.findById(r.roundId()).orElseThrow(() -> ApiException.notFound("Round not found: " + r.roundId()));
            if (!round.getTrack().getId().equals(tm.getTrack().getId()))
                throw ApiException.badRequest("Round must belong to mentor track");
        }
        return MentorFeedbackMapper.toResponse(repo.save(MentorFeedback.builder().trackMentor(tm).team(team).round(round).content(r.content().trim()).build()));
    }

    @Transactional
    public MentorFeedbackResponse update(UUID id, UpdateMentorFeedbackRequest r, Authentication auth) {
        MentorFeedback f = find(id);
        authorizeMentorOrCoordinator(f.getTrackMentor(), auth);
        f.setContent(r.content().trim());
        return MentorFeedbackMapper.toResponse(f);
    }

    @Transactional
    public void delete(UUID id, Authentication auth) {
        MentorFeedback f = find(id);
        authorizeMentorOrCoordinator(f.getTrackMentor(), auth);
        repo.delete(f);
    }

    private MentorFeedback find(UUID id) {
        return repo.findWithRelationsById(id).orElseThrow(() -> ApiException.notFound("Mentor feedback not found: " + id));
    }

    private void authorizeMentorOrCoordinator(TrackMentor tm, Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof CurrentUser c))
            throw ApiException.forbidden("Authentication required");
        boolean coord = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_COORDINATOR"));
        if (!coord && !tm.getUser().getId().equals(c.getId()))
            throw ApiException.forbidden("Only assigned mentor or coordinator can modify feedback");
    }
}
