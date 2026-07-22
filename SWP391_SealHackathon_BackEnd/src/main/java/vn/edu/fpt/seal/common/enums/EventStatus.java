package vn.edu.fpt.seal.common.enums;

/**
 * Trạng thái vòng đời của một sự kiện hackathon (event).
 */
public enum EventStatus {
    /** Bản nháp, chưa công khai. */
    draft,
    /** Đã công bố, người dùng có thể xem/đăng ký. */
    published,
    /** Đang diễn ra. */
    ongoing,
    /** Đã kết thúc. */
    completed,
    /** Đã hủy. */
    cancelled
}
