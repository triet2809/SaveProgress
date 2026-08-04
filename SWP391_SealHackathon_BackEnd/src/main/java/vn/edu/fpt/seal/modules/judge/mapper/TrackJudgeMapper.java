package vn.edu.fpt.seal.modules.judge.mapper;

import vn.edu.fpt.seal.modules.judge.dto.TrackJudgeResponse;
import vn.edu.fpt.seal.modules.judge.entity.TrackJudge;

public final class TrackJudgeMapper {
    private TrackJudgeMapper() {
    }

    public static TrackJudgeResponse toResponse(TrackJudge tj) {
        return TrackJudgeResponse.builder().id(tj.getId()).eventId(tj.getEvent().getId()).trackId(tj.getTrack().getId()).trackName(tj.getTrack().getName()).userId(tj.getUser().getId()).email(tj.getUser().getEmail()).fullName(tj.getUser().getFullName()).assignedAt(tj.getAssignedAt()).build();
    }
}
