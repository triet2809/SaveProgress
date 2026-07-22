package vn.edu.fpt.seal.modules.event.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * DTO cập nhật sự kiện. Các trường null sẽ được giữ nguyên (cập nhật một phần).
 *
 * @param title             tiêu đề mới, tối đa 255 ký tự
 * @param description       mô tả mới, tối đa 10000 ký tự
 * @param term              học kỳ/kỳ tổ chức
 * @param prizePool         tổng giải thưởng
 * @param registrationStart thời điểm mở đăng ký
 * @param registrationEnd   thời điểm đóng đăng ký
 * @param eventStart        thời điểm bắt đầu sự kiện
 * @param eventEnd          thời điểm kết thúc sự kiện
 */
public record UpdateEventRequest(
        @Size(max = 255) String title,
        @Size(max = 10000) String description,
        @Size(max = 255) String term,
        @Size(max = 255) String prizePool,
        LocalDateTime registrationStart,
        LocalDateTime registrationEnd,
        LocalDateTime eventStart,
        LocalDateTime eventEnd
) {
}
