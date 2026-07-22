package vn.edu.fpt.seal.common.enums;

/**
 * Trạng thái duyệt tài khoản người dùng trong hệ thống.
 * Dùng để kiểm soát người dùng nào được phép đăng nhập/hoạt động.
 */
public enum AccountStatus {
    /** Tài khoản mới đăng ký, đang chờ coordinator duyệt. */
    pending,
    /** Tài khoản đã được duyệt, được phép truy cập hệ thống. */
    approved,
    /** Tài khoản bị từ chối, không được phép truy cập. */
    rejected
}
