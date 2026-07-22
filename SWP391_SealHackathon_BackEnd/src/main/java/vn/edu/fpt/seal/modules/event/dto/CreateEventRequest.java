package vn.edu.fpt.seal.modules.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * DTO tạo mới sự kiện.
 *
 * @param title             tiêu đề, bắt buộc, tối đa 255 ký tự
 * @param description       mô tả, tối đa 10000 ký tự
 * @param term              học kỳ/kỳ tổ chức
 * @param prizePool         tổng giải thưởng
 * @param registrationStart thời điểm mở đăng ký
 * @param registrationEnd   thời điểm đóng đăng ký
 * @param eventStart        thời điểm bắt đầu sự kiện
 * @param eventEnd          thời điểm kết thúc sự kiện
 */
public record CreateEventRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 10000) String description,
        @Size(max = 255) String term,
        @Size(max = 255) String prizePool,
        LocalDateTime registrationStart,
        LocalDateTime registrationEnd,
        LocalDateTime eventStart,
        LocalDateTime eventEnd
) {
}
