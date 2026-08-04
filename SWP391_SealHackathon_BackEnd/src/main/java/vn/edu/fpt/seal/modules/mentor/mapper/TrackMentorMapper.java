package vn.edu.fpt.seal.modules.mentor.mapper;

import vn.edu.fpt.seal.modules.mentor.dto.TrackMentorResponse;
import vn.edu.fpt.seal.modules.mentor.entity.TrackMentor;

public final class TrackMentorMapper {
    private TrackMentorMapper() {
    }

    public static TrackMentorResponse toResponse(TrackMentor tm) {
        return TrackMentorResponse.builder().id(tm.getId()).eventId(tm.getEvent().getId()).trackId(tm.getTrack().getId()).trackName(tm.getTrack().getName()).userId(tm.getUser().getId()).email(tm.getUser().getEmail()).fullName(tm.getUser().getFullName()).assignedAt(tm.getAssignedAt()).build();
    }
}
