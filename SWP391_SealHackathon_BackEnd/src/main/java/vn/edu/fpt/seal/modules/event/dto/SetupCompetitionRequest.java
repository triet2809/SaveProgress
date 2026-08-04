package vn.edu.fpt.seal.modules.event.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * One-shot "build the competition" wizard input (fired when registration closes).
 * <p>
 * - tracks: the thematic groups to create. If null/empty, a single "General" track
 * is used and every registered team stays in it.
 * - finalistCount (F): desired number of finalists PER TRACK. If null, defaults to
 * max(3, ceil(0.1 * teamsInTrack)).
 * - roundCount (R): number of elimination rounds PER TRACK. If null, suggested from
 * team count: &lt;=8 -&gt; 1, 9-30 -&gt; 2, 31-80 -&gt; 3, &gt;80 -&gt; 4.
 */

/**
 * Dữ liệu đầu vào cho wizard "dựng cuộc thi" một lần (chạy khi đóng đăng ký).
 * <p>
 * One-shot "build the competition" wizard input (fired when registration closes).
 * <p>
 * - tracks: các nhóm chủ đề cần tạo. Nếu null/rỗng, dùng một track "General" duy nhất
 *   và mọi đội đã đăng ký ở trong đó.
 * - finalistCount (F): số đội vào chung kết MONG MUỐN MỖI TRACK. Nếu null, mặc định
 *   max(3, ceil(0.1 * số đội trong track)).
 * - roundCount (R): số vòng loại MỖI TRACK. Nếu null, gợi ý theo số đội:
 *   &lt;=8 -&gt; 1, 9-30 -&gt; 2, 31-80 -&gt; 3, &gt;80 -&gt; 4.
 *
 * @param tracks        danh sách track cần tạo
 * @param finalistCount số đội vào chung kết mỗi track (tối thiểu 1)
 * @param roundCount    số vòng loại mỗi track (tối thiểu 1)
 * @param roundPlan     kế hoạch vòng thi chi tiết (tùy chọn)
 */
public record SetupCompetitionRequest(
        List<TrackSpec> tracks,
        @Min(1) Integer finalistCount,
        @Min(1) Integer roundCount,
        List<LogicalRoundSpec> roundPlan
) {
    /**
     * Constructor rút gọn không có roundPlan (tương thích ngược).
     */
    public SetupCompetitionRequest(List<TrackSpec> tracks, Integer finalistCount, Integer roundCount) {
        this(tracks, finalistCount, roundCount, null);
    }

    /**
     * Đặc tả một track cần tạo.
     *
     * @param name        tên track, bắt buộc, tối đa 255 ký tự
     * @param description mô tả track, tối đa 10000 ký tự
     */
    public record TrackSpec(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 10000) String description
    ) {
    }

    /**
     * Đặc tả một vòng thi logic (logical round) trải trên nhiều track.
     *
     * @param name                 tên vòng, bắt buộc, tối đa 255 ký tự
     * @param sequenceNumber       thứ tự vòng (tối thiểu 1)
     * @param finalRound           có phải vòng chung kết hay không
     * @param defaultTopNToPromote số đội mặc định được đi tiếp (tối thiểu 1)
     * @param tracks               danh sách track thuộc vòng này
     */
    public record LogicalRoundSpec(
            @NotBlank @Size(max = 255) String name,
            @Min(1) Integer sequenceNumber,
            Boolean finalRound,
            @Min(1) Integer defaultTopNToPromote,
            List<RoundTrackSpec> tracks
    ) {
    }

    /**
     * Đặc tả track trong phạm vi một vòng thi.
     *
     * @param name          tên track, bắt buộc, tối đa 255 ký tự
     * @param description   mô tả, tối đa 10000 ký tự
     * @param topNToPromote số đội được đi tiếp từ track này (tối thiểu 1)
     */
    public record RoundTrackSpec(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 10000) String description,
            @Min(1) Integer topNToPromote
    ) {
    }
}
