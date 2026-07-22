package vn.edu.fpt.seal.common.enums;

/**
 * Trạng thái xử lý của một báo cáo sự cố (incident report).
 */
public enum IncidentStatus {
    /** Đã được báo cáo, chưa xử lý. */
    reported,
    /** Đang được xem xét. */
    under_review,
    /** Đã giải quyết. */
    resolved,
    /** Đã bị từ chối. */
    rejected
}
