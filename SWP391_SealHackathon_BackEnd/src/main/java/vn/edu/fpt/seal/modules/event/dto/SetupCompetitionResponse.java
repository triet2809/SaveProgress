package vn.edu.fpt.seal.modules.event.dto;

import lombok.Builder;
import vn.edu.fpt.seal.modules.seeding.dto.SeedingDtos;

import java.util.List;
import java.util.UUID;

/**
 * Result of the one-shot competition setup: what tracks/rounds were generated and
 * how the registered teams were distributed.
 * <p>
 * Kết quả của bước dựng cuộc thi một lần: các track/vòng được sinh ra và
 * cách phân bổ các đội đã đăng ký.
 *
 * @param eventId        ID sự kiện
 * @param totalTeams     tổng số đội đã đăng ký
 * @param trackCount     số track được tạo
 * @param roundsPerTrack số vòng mỗi track
 * @param seedReview     tóm tắt kết quả seeding (xếp đội vào track/vòng)
 * @param warnings       danh sách cảnh báo phát sinh khi dựng
 * @param tracks         kế hoạch chi tiết từng track
 */
@Builder
public record SetupCompetitionResponse(
        UUID eventId,
        int totalTeams,
        int trackCount,
        int roundsPerTrack,
        SeedingDtos.ReviewSummary seedReview,
        List<String> warnings,
        List<TrackPlan> tracks
) {
    /**
     * Kế hoạch cho một track: thông tin track và các vòng bên trong.
     *
     * @param trackId   ID track
     * @param name      tên track
     * @param teamCount số đội trong track
     * @param rounds    danh sách vòng của track
     */
    @Builder
    public record TrackPlan(
            UUID trackId,
            String name,
            int teamCount,
            List<RoundPlan> rounds
    ) {
    }

    /**
     * Kế hoạch cho một vòng thi.
     *
     * @param roundId            ID vòng
     * @param name               tên vòng
     * @param sequenceNumber     thứ tự vòng
     * @param topNToPromote      số đội được đi tiếp
     * @param seededParticipants số đội được xếp vào vòng này
     */
    @Builder
    public record RoundPlan(
            UUID roundId,
            String name,
            int sequenceNumber,
            int topNToPromote,
            int seededParticipants
    ) {
    }
}
