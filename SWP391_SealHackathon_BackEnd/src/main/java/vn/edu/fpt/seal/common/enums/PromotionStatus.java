package vn.edu.fpt.seal.common.enums;

/**
 * Trạng thái thăng hạng của đội thi qua các vòng.
 */
public enum PromotionStatus {
    /**
     * Chưa xét.
     */
    pending,
    /**
     * Được thăng lên vòng tiếp theo.
     */
    promoted,
    /**
     * Bị loại.
     */
    eliminated,
    /**
     * Bị truất quyền.
     */
    disqualified
}
