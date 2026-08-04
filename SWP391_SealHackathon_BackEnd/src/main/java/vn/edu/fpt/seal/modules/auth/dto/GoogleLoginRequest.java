package vn.edu.fpt.seal.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import vn.edu.fpt.seal.common.enums.StudentType;

import java.util.UUID;

/**
 * Yêu cầu đăng nhập/đăng ký bằng Google.
 * Chứa ID token (JWT) do Google Identity Services cấp ở phía frontend.
 *
 * <p>Đăng nhập Google diễn ra 2 pha:</p>
 * <ol>
 *   <li>Pha 1: chỉ gửi {@code idToken}. Nếu là người dùng mới, backend chưa tạo
 *       tài khoản mà trả về cờ {@code profileCompletionRequired} để frontend
 *       thu thập thêm loại sinh viên, MSSV và campus.</li>
 *   <li>Pha 2: gửi lại {@code idToken} kèm {@code studentType}, {@code studentId},
 *       {@code campusId}/{@code universityName} để tạo tài khoản pending đầy đủ.</li>
 * </ol>
 *
 * @param idToken        Google ID token cần được backend xác minh
 * @param studentType    loại sinh viên (fpt / external), gửi ở pha 2
 * @param studentId      mã số sinh viên (nếu có), gửi ở pha 2
 * @param universityId   ID trường đã có trong hệ thống (tùy chọn)
 * @param universityName tên trường (dùng cho sinh viên external), gửi ở pha 2
 * @param campusId       ID campus của trường (dùng cho sinh viên FPT), gửi ở pha 2
 */
public record GoogleLoginRequest(
        @NotBlank(message = "Google ID token is required")
        String idToken,
        StudentType studentType,
        String studentId,
        UUID universityId,
        String universityName,
        UUID campusId
) {
}
