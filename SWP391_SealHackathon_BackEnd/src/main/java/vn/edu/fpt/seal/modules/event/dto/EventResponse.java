package vn.edu.fpt.seal.modules.event.dto;

import lombok.Builder;
import vn.edu.fpt.seal.common.enums.EventStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO phản hồi thông tin sự kiện (event) trả về cho client.
 * Gồm thông tin cơ bản và các số liệu thống kê (số vòng, số track, số người tham gia).
 *
 * @param id                ID sự kiện
 * @param title             tiêu đề sự kiện
 * @param description       mô tả sự kiện
 * @param status            trạng thái sự kiện
 * @param term              học kỳ/kỳ tổ chức
 * @param prizePool         tổng giải thưởng
 * @param registrationStart thời điểm mở đăng ký
 * @param registrationEnd   thời điểm đóng đăng ký
 * @param eventStart        thời điểm bắt đầu sự kiện
 * @param eventEnd          thời điểm kết thúc sự kiện
 * @param roundsCount       tổng số vòng thi
 * @param tracksCount       tổng số track
 * @param participantsCount tổng số đội/người tham gia
 * @param createdAt         thời điểm tạo
 * @param updatedAt         thời điểm cập nhật gần nhất
 */
@Builder
public record EventResponse(
        UUID id,
        String title,
        String description,
        EventStatus status,
        String term,
        String prizePool,
        LocalDateTime registrationStart,
        LocalDateTime registrationEnd,
        LocalDateTime eventStart,
        LocalDateTime eventEnd,
        Integer roundsCount,
        Integer tracksCount,
        Long participantsCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
