package vn.edu.fpt.seal.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO chứa refresh token để cấp lại access token mới khi token cũ hết hạn.
 *
 * @param refreshToken chuỗi refresh token hợp lệ, bắt buộc không được rỗng
 */
public record RefreshRequest(@NotBlank String refreshToken) {
}
