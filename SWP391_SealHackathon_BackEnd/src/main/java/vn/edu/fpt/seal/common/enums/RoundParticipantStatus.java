package vn.edu.fpt.seal.common.enums;

/**
 * Trạng thái tham gia của một đội trong một vòng thi cụ thể.
 */
public enum RoundParticipantStatus {
    /** Chờ xác nhận tham gia. */
    pending,
    /** Đang tham gia tích cực. */
    active,
    /** Đã được thăng lên vòng sau. */
    promoted,
    /** Bị loại khỏi vòng. */
    eliminated,
    /** Bị truất quyền. */
    disqualified
}
