package vn.edu.fpt.seal.modules.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

/**
 * DTO hoàn tất onboarding: đổi mật khẩu (tùy chọn) và xác nhận điều khoản.
 *
 * @param newPassword     mật khẩu mới (tùy chọn), nếu có thì dài 8-72 ký tự
 * @param confirmPassword nhập lại mật khẩu mới để xác nhận
 * @param acceptedTerms   cờ đồng ý Điều khoản & Chính sách, bắt buộc phải là true
 */
public record CompleteOnboardingRequest(
        @Size(min = 8, max = 72) String newPassword,
        String confirmPassword,
        @jakarta.validation.constraints.NotNull
        @AssertTrue(message = "Terms and Privacy Policy acceptance is required") Boolean acceptedTerms) {
}
