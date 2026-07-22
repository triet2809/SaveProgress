package vn.edu.fpt.seal.common.enums;

/**
 * Phân loại sự cố (incident) có thể xảy ra trong cuộc thi.
 */
public enum IncidentType {
    /** Gian lận. */
    cheating,
    /** Đạo văn. */
    plagiarism,
    /** Bài nộp không hợp lệ. */
    invalid_submission,
    /** Vi phạm luật thi. */
    rule_violation,
    /** Sự cố kỹ thuật. */
    technical_issue,
    /** Loại khác. */
    other
}
