package vn.edu.fpt.seal.modules.score.mapper;

import vn.edu.fpt.seal.modules.score.dto.ScoreResponse; import vn.edu.fpt.seal.modules.score.entity.Score;
public final class ScoreMapper { private ScoreMapper(){} public static ScoreResponse toResponse(Score s){return ScoreResponse.builder().id(s.getId()).submissionId(s.getSubmission().getId()).judgeId(s.getJudge().getId()).judgeEmail(s.getJudge().getEmail()).criterionId(s.getCriterion().getId()).criterionName(s.getCriterion().getName()).score(s.getScore()).weightedScore(s.getWeightedScore()).comment(s.getComment()).createdAt(s.getCreatedAt()).updatedAt(s.getUpdatedAt()).build();}}
