package vn.edu.fpt.seal.modules.judge.mapper;

import vn.edu.fpt.seal.modules.judge.dto.RoundJudgeResponse;
import vn.edu.fpt.seal.modules.judge.entity.RoundJudge;

public final class RoundJudgeMapper {
    private RoundJudgeMapper() {
    }

    public static RoundJudgeResponse toResponse(RoundJudge rj) {
        return RoundJudgeResponse.builder().id(rj.getId()).roundId(rj.getRound().getId()).roundName(rj.getRound().getName()).userId(rj.getUser().getId()).email(rj.getUser().getEmail()).fullName(rj.getUser().getFullName()).assignedAt(rj.getAssignedAt()).build();
    }
}
