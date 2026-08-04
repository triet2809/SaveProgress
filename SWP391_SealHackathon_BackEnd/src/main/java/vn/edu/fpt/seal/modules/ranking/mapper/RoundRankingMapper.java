package vn.edu.fpt.seal.modules.ranking.mapper;

import vn.edu.fpt.seal.modules.ranking.dto.RoundRankingResponse;
import vn.edu.fpt.seal.modules.ranking.entity.RoundRanking;
import vn.edu.fpt.seal.modules.recognition.dto.RecognitionDtos;

import java.util.List;

public final class RoundRankingMapper {
    private RoundRankingMapper() {
    }

    public static RoundRankingResponse toResponse(RoundRanking r) {
        return toResponse(r, List.of());
    }

    public static RoundRankingResponse toResponse(
            RoundRanking r, List<RecognitionDtos.Summary> recognitions) {
        return RoundRankingResponse.builder()
                .id(r.getId()).roundId(r.getRound().getId()).trackId(r.getRound().getTrack().getId())
                .trackName(r.getRound().getTrack().getName()).teamId(r.getTeam().getId()).teamName(r.getTeam().getName())
                .totalScore(r.getTotalScore()).rank(r.getRank()).status(r.getStatus())
                .resultPublishedAt(r.getRound().getResultPublishedAt())
                .tieBreakerCriterionId(r.getTieBreakerCriterion() == null ? null : r.getTieBreakerCriterion().getId())
                .tieBreakerScore(r.getTieBreakerScore()).tieBreakerReason(r.getTieBreakerReason())
                .calculatedAt(r.getCalculatedAt()).updatedAt(r.getUpdatedAt())
                .recognitions(recognitions == null ? List.of() : recognitions).build();
    }
}
