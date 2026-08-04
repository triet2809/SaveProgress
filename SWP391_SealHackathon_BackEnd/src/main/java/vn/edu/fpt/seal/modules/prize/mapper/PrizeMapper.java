package vn.edu.fpt.seal.modules.prize.mapper;

import vn.edu.fpt.seal.modules.prize.dto.PrizeResponse;
import vn.edu.fpt.seal.modules.prize.entity.Prize;

public final class PrizeMapper {
    private PrizeMapper() {
    }

    public static PrizeResponse toResponse(Prize p) {
        return PrizeResponse.builder().id(p.getId()).eventId(p.getEvent().getId()).trackId(p.getTrack() == null ? null : p.getTrack().getId()).trackName(p.getTrack() == null ? null : p.getTrack().getName()).teamId(p.getTeam() == null ? null : p.getTeam().getId()).teamName(p.getTeam() == null ? null : p.getTeam().getName()).name(p.getName()).prizeAmount(p.getPrizeAmount()).description(p.getDescription()).awardedAt(p.getAwardedAt()).createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt()).build();
    }
}
