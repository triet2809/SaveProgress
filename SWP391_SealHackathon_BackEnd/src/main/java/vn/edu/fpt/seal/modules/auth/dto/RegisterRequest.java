package vn.edu.fpt.seal.modules.auth.dto;

import jakarta.validation.constraints.*;
import vn.edu.fpt.seal.common.enums.StudentType;

import java.util.UUID;

/**
 * DTO nhận dữ liệu đăng ký tài khoản người dùng mới.
 * Dùng ở endpoint đăng ký của {@code AuthController}, được validate bằng Jakarta Bean Validation.
 *
 * @param email          email đăng nhập, bắt buộc và đúng định dạng email
 * @param password       mật khẩu, bắt buộc, dài 8-128 ký tự
 * @param fullName       họ tên đầy đủ, bắt buộc, tối đa 255 ký tự
 * @param studentType    loại sinh viên (FPT / ngoài FPT...), xác định luồng xác thực
 * @param studentId      mã số sinh viên (nếu có)
 * @param universityId   ID trường đại học đã có trong hệ thống
 * @param universityName tên trường (dùng khi trường chưa có trong hệ thống), tối đa 255 ký tự
 * @param campusId       ID cơ sở/campus của trường
 * @param acceptedTerms  cờ đồng ý Điều khoản & Chính sách, bắt buộc phải là true
 */
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank @Size(max = 255) String fullName,
        StudentType studentType,
        String studentId,
        UUID universityId,
        @Size(max = 255) String universityName,
        UUID campusId,
        @NotNull @AssertTrue(message = "Terms and Privacy Policy acceptance is required") Boolean acceptedTerms
) {
}
