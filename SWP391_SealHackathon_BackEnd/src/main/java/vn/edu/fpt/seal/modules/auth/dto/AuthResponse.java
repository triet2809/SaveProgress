package vn.edu.fpt.seal.modules.auth.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * DTO phản hồi sau khi đăng nhập/đăng ký thành công.
 * Chứa cặp token JWT và thông tin tóm tắt người dùng để client dùng ngay.
 *
 * @param accessToken  JWT access token dùng để gọi API được bảo vệ
 * @param refreshToken token dùng để làm mới access token
 * @param user         thông tin tóm tắt người dùng đã đăng nhập
 */
@Builder
public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserSummary user
) {
    /**
     * Thông tin tóm tắt người dùng kèm trong phản hồi xác thực.
     *
     * @param id                        ID người dùng
     * @param email                     email đăng nhập
     * @param fullName                  họ tên đầy đủ
     * @param status                    trạng thái tài khoản (ACTIVE, PENDING...)
     * @param studentType               loại sinh viên
     * @param universityId              ID trường đại học
     * @param universityName            tên trường đại học
     * @param campusId                  ID cơ sở/campus
     * @param campusName                tên cơ sở/campus
     * @param isGuest                   true nếu là tài khoản khách (guest)
     * @param roles                     danh sách vai trò (quyền) của người dùng
     * @param mustChangePassword        true nếu bắt buộc đổi mật khẩu ở lần đăng nhập tới
     * @param termsAcceptanceRequired   true nếu cần chấp nhận điều khoản mới
     * @param onboardingRequired        true nếu cần hoàn tất bước onboarding
     * @param profileCompletionRequired true nếu là tài khoản Google mới cần bổ sung MSSV/campus trước khi tạo
     */
    @Builder
    public record UserSummary(
            UUID id,
            String email,
            String fullName,
            String status,
            String studentType,
            UUID universityId,
            String universityName,
            UUID campusId,
            String campusName,
            boolean isGuest,
            List<String> roles,
            boolean mustChangePassword,
            boolean termsAcceptanceRequired,
            boolean onboardingRequired,
            boolean profileCompletionRequired
    ) {
    }
}
