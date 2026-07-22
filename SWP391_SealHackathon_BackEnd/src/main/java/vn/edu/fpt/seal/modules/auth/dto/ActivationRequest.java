package vn.edu.fpt.seal.modules.auth.dto;

import jakarta.validation.constraints.*;

/**
 * DTO nhận dữ liệu kích hoạt tài khoản: đặt mật khẩu lần đầu qua token kích hoạt.
 *
 * @param token           token kích hoạt gửi qua email, bắt buộc không rỗng
 * @param password        mật khẩu mới, bắt buộc, dài 8-72 ký tự
 * @param confirmPassword nhập lại mật khẩu để xác nhận, bắt buộc không rỗng
 * @param acceptedTerms   cờ đồng ý Điều khoản & Chính sách, bắt buộc phải là true
 */
public record ActivationRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank String confirmPassword,
        @NotNull @AssertTrue(message = "Terms and Privacy Policy acceptance is required") Boolean acceptedTerms) {}
