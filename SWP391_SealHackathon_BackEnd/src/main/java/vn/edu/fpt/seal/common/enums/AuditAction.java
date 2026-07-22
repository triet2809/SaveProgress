package vn.edu.fpt.seal.common.enums;

/**
 * Loại hành động được ghi lại trong nhật ký audit (audit log).
 * Mỗi giá trị đại diện cho một sự kiện nghiệp vụ cần truy vết để kiểm toán.
 */
public enum AuditAction {
    // Hành động CRUD cơ bản trên tài nguyên
    CREATE, UPDATE, DELETE,
    // Duyệt / từ chối tài khoản người dùng
    APPROVE_USER, REJECT_USER,
    // Nộp bài, chấm điểm và cập nhật điểm
    SUBMIT_PROJECT, SCORE_SUBMISSION, UPDATE_SCORE,
    // Truất quyền, thăng hạng, loại đội thi
    DISQUALIFY_TEAM, PROMOTE_TEAM, ELIMINATE_TEAM,
    // Vòng đời của báo cáo sự cố (incident)
    CREATE_INCIDENT, PROCESS_INCIDENT, RESOLVE_INCIDENT, REJECT_INCIDENT,
    // Công bố kết quả
    PUBLISH_RESULTS,
    // Chốt kết quả và gán/gỡ hạt giống (seed)
    FINALIZE_RESULTS, ASSIGN_SEED, REMOVE_SEED,
    // Trao / thu hồi / khôi phục giải thưởng đội thi
    TEAM_RECOGNITION_AWARDED, TEAM_RECOGNITION_REVOKED, TEAM_RECOGNITION_RESTORED
}
