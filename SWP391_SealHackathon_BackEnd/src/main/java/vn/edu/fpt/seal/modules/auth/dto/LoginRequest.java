package vn.edu.fpt.seal.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO nhận thông tin đăng nhập.
 *
 * @param email    email đăng nhập, bắt buộc và đúng định dạng email
 * @param password mật khẩu, bắt buộc không rỗng
 */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {}
