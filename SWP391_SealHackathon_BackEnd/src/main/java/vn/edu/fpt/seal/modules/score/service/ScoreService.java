package vn.edu.fpt.seal.modules.score.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.criteria.entity.RoundCriterion;
import vn.edu.fpt.seal.modules.criteria.repository.RoundCriterionRepository;
import vn.edu.fpt.seal.modules.judge.repository.RoundJudgeRepository;
import vn.edu.fpt.seal.modules.score.dto.*;
import vn.edu.fpt.seal.modules.score.entity.Score;
import vn.edu.fpt.seal.modules.score.mapper.ScoreMapper;
import vn.edu.fpt.seal.modules.score.repository.ScoreRepository;
import vn.edu.fpt.seal.modules.submission.entity.Submission;
import vn.edu.fpt.seal.modules.submission.repository.SubmissionRepository;
import vn.edu.fpt.seal.modules.user.entity.User;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.security.CurrentUser;
import vn.edu.fpt.seal.security.AuthorizationService;
import java.math.*;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class ScoreService {
    private final ScoreRepository scoreRepository; private final SubmissionRepository submissionRepository; private final RoundCriterionRepository criterionRepository; private final UserRepository userRepository; private final RoundJudgeRepository roundJudgeRepository; private final AuthorizationService authorizationService;
    @Transactional(readOnly=true) public Page<ScoreResponse> list(UUID submissionId, UUID judgeId, Pageable pageable, Authentication auth){
        Page<Score> scores = submissionId!=null ? scoreRepository.findBySubmissionId(submissionId,pageable) : judgeId!=null ? scoreRepository.findByJudgeId(judgeId,pageable) : scoreRepository.findAll(pageable);
        CurrentUser user=authorizationService.current(auth);
        scores.forEach(score->authorizationService.require(authorizationService.canReadDetailedScore(user,score.getSubmission()),"You are not authorized to view one or more requested scores"));
        return scores.map(ScoreMapper::toResponse);
    }
    @Transactional(readOnly=true) public ScoreResponse get(UUID id, Authentication auth){Score score=findOrThrow(id); authorizationService.require(authorizationService.canReadDetailedScore(authorizationService.current(auth),score.getSubmission()),"You are not authorized to view this score"); return ScoreMapper.toResponse(score);}
    @Transactional public ScoreResponse upsert(UpsertScoreRequest req, Authentication auth){
        CurrentUser current=current(auth); UUID judgeId=current.getId();
        Submission sub=submissionRepository.findWithRelationsById(req.submissionId()).orElseThrow(()->ApiException.notFound("Submission not found: "+req.submissionId()));
        RoundCriterion criterion=criterionRepository.findWithRoundById(req.criterionId()).orElseThrow(()->ApiException.notFound("Criterion not found: "+req.criterionId()));
        if(!criterion.getRound().getId().equals(sub.getRound().getId())) throw ApiException.badRequest("Criterion must belong to the submission round");
        boolean coordinator=auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_COORDINATOR"));
        if(!coordinator && !roundJudgeRepository.existsByRoundIdAndUserId(sub.getRound().getId(), judgeId)) throw ApiException.forbidden("Judge is not assigned to this submission round");
        User judge=userRepository.findById(judgeId).orElseThrow(()->ApiException.notFound("Judge not found: "+judgeId));
        Score score=scoreRepository.findBySubmissionIdAndJudgeIdAndCriterionId(sub.getId(),judgeId,criterion.getId()).orElseGet(()->Score.builder().submission(sub).judge(judge).criterion(criterion).build());
        score.setScore(req.score()); score.setWeightedScore(req.score().multiply(criterion.getWeight()).setScale(2, RoundingMode.HALF_UP)); score.setComment(req.comment()==null?score.getComment():req.comment().trim());
        return ScoreMapper.toResponse(scoreRepository.save(score));
    }
    @Transactional public void delete(UUID id, Authentication auth){ Score s=findOrThrow(id); CurrentUser cur=current(auth); boolean coordinator=auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_COORDINATOR")); if(!coordinator && !s.getJudge().getId().equals(cur.getId())) throw ApiException.forbidden("Only owner judge or coordinator can delete score"); scoreRepository.delete(s); }
    private Score findOrThrow(UUID id){return scoreRepository.findWithRelationsById(id).orElseThrow(()->ApiException.notFound("Score not found: "+id));}
    private CurrentUser current(Authentication auth){ if(auth==null||!(auth.getPrincipal() instanceof CurrentUser c)) throw ApiException.forbidden("Authentication required"); return c; }
}
