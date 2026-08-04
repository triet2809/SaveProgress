package vn.edu.fpt.seal.common.enums;

/**
 * Loại hành động xử lý áp dụng khi giải quyết một báo cáo sự cố (incident).
 */
public enum IncidentActionType {
    /**
     * Cảnh cáo đội thi.
     */
    warning,
    /**
     * Yêu cầu nộp lại bài.
     */
    require_resubmission,
    /**
     * Điều chỉnh điểm.
     */
    score_adjustment,
    /**
     * Truất quyền đội thi.
     */
    disqualify_team,
    /**
     * Từ chối báo cáo.
     */
    reject_report,
    /**
     * Hành động khác.
     */
    other
}
