package vn.edu.fpt.seal.modules.track.mapper;

import vn.edu.fpt.seal.modules.track.dto.TrackResponse;
import vn.edu.fpt.seal.modules.track.entity.Track;

public final class TrackMapper {

    private TrackMapper() {
    }

    public static TrackResponse toResponse(Track t) {
        return TrackResponse.builder()
                .id(t.getId())
                .eventId(t.getEvent().getId())
                .name(t.getName())
                .description(t.getDescription())
                .maxTeams(t.getMaxTeams())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
