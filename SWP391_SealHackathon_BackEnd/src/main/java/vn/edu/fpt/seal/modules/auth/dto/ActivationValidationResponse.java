package vn.edu.fpt.seal.modules.auth.dto;

/**
 * DTO trả về kết quả kiểm tra tính hợp lệ của token kích hoạt tài khoản.
 *
 * @param valid   true nếu token còn hợp lệ (chưa hết hạn, chưa dùng)
 * @param message thông báo mô tả kết quả (lý do không hợp lệ nếu có)
 */
public record ActivationValidationResponse(boolean valid, String message) {
}
