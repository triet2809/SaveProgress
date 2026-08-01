package vn.edu.fpt.seal.modules.auth.service;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.enums.AccountStatus;
import vn.edu.fpt.seal.common.enums.RoleName;
import vn.edu.fpt.seal.common.enums.StudentType;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.auth.dto.AuthResponse;
import vn.edu.fpt.seal.modules.auth.dto.GoogleLoginRequest;
import vn.edu.fpt.seal.modules.auth.dto.LoginRequest;
import vn.edu.fpt.seal.modules.auth.dto.RefreshRequest;
import vn.edu.fpt.seal.modules.auth.dto.RegisterRequest;
import vn.edu.fpt.seal.modules.university.entity.Campus;
import vn.edu.fpt.seal.modules.university.entity.University;
import vn.edu.fpt.seal.modules.university.repository.CampusRepository;
import vn.edu.fpt.seal.modules.university.repository.UniversityRepository;
import vn.edu.fpt.seal.modules.user.entity.Role;
import vn.edu.fpt.seal.modules.user.entity.User;
import vn.edu.fpt.seal.modules.user.repository.RoleRepository;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.security.JwtService;
import vn.edu.fpt.seal.security.GoogleTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import vn.edu.fpt.seal.config.AppProperties;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * Service xử lý nghiệp vụ xác thực: đăng ký, đăng nhập, đăng xuất,
 * làm mới token và hoàn tất onboarding.
 * Phối hợp {@link JwtService} để cấp/thu hồi JWT và các repository để truy xuất dữ liệu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CampusRepository campusRepository;
    private final UniversityRepository universityRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final AppProperties appProperties;

    /**
     * Đăng ký tài khoản mới ở trạng thái pending (chờ duyệt).
     * Phân giải trường/campus theo dữ liệu đầu vào, gán vai trò mặc định TEAM_MEMBER.
     *
     * @param req dữ liệu đăng ký
     * @return phản hồi trạng thái pending (không kèm token vì chưa duyệt)
     * @throws ApiException nếu email đã tồn tại, trường/campus không tìm thấy,
     *                      hoặc thiếu tên trường cho sinh viên ngoài FPT
     */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        // Chuẩn hóa email (lưu chữ thường, bỏ khoảng trắng) trước khi kiểm tra trùng
        if (userRepository.existsByEmail(req.email().toLowerCase().trim())) {
            throw ApiException.conflict("Email already registered");
        }

        StudentType type = req.studentType() != null ? req.studentType() : StudentType.none;
        Campus campus = null;
        University university = null;

        // Ưu tiên campusId; nếu không có thì universityId; nếu là sinh viên external thì
        // tạo/tìm trường theo tên
        if (req.campusId() != null) {
            campus = campusRepository.findWithUniversityById(req.campusId())
                    .orElseThrow(() -> ApiException.notFound("Campus not found"));
            university = campus.getUniversity();
        } else if (req.universityId() != null) {
            university = universityRepository.findById(req.universityId())
                    .orElseThrow(() -> ApiException.notFound("University not found"));
        } else if (type == StudentType.external) {
            String universityName = req.universityName() == null ? null : req.universityName().trim();
            if (universityName == null || universityName.isBlank()) {
                throw ApiException.badRequest("University name is required for external students");
            }
            // Tạo mới trường nếu chưa tồn tại trong hệ thống
            university = universityRepository.findByNameIgnoreCase(universityName)
                    .orElseGet(() -> universityRepository.save(University.builder()
                            .name(universityName)
                            .country("Vietnam")
                            .build()));
        }

        Role defaultRole = roleRepository.findByName(RoleName.TEAM_MEMBER)
                .orElseThrow(() -> new IllegalStateException("Default role team_member not seeded"));

        User user = User.builder()
                .email(req.email().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName().trim())
                .studentType(type)
                .studentId(req.studentId())
                .university(university)
                .campus(campus)
                .isGuest(false)
                .status(AccountStatus.pending)
                .termsAcceptedAt(LocalDateTime.now(ZoneOffset.UTC))
                .termsVersion(appProperties.getLegal().getTermsVersion())
                .privacyVersion(appProperties.getLegal().getPrivacyVersion())
                .roles(new HashSet<>(List.of(defaultRole)))
                .build();

        user = userRepository.save(user);
        log.info("User registered (pending approval): {}", user.getEmail());

        return buildPendingResponse(user);
    }

    /**
     * Đăng nhập/đăng ký bằng Google ID token.
     * Xác minh token với Google, sau đó:
     * <ul>
     *   <li>Nếu đã có tài khoản theo googleSub → đăng nhập.</li>
     *   <li>Nếu email đã tồn tại (tài khoản local) → liên kết googleSub vào tài khoản đó.</li>
     *   <li>Nếu chưa có → tạo mới ở trạng thái pending (chờ duyệt).</li>
     * </ul>
     *
     * @param req yêu cầu Google (idToken + hồ sơ bổ sung ở pha 2)
     * @return phản hồi xác thực (kèm token nếu approved, pending nếu chưa duyệt,
     *         hoặc cờ profileCompletionRequired nếu là người dùng mới cần bổ sung hồ sơ)
     * @throws ApiException nếu token không hợp lệ hoặc tài khoản bị từ chối
     */
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest req) {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(req.idToken());
        String sub = payload.getSubject();
        String email = payload.getEmail() == null ? null : payload.getEmail().toLowerCase().trim();
        Object nameClaim = payload.get("name");
        String fullName = nameClaim != null ? nameClaim.toString().trim() : (email != null ? email : "Google User");

        if (email == null || email.isBlank()) {
            throw ApiException.unauthorized("Google account has no email");
        }

        // 1) Tìm theo googleSub trước (đăng nhập lần sau)
        User user = userRepository.findByGoogleSub(sub).orElse(null);

        // 2) Chưa có googleSub → thử liên kết theo email (tài khoản local cũ)
        if (user == null) {
            user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                // Liên kết danh tính Google vào tài khoản hiện có
                user.setGoogleSub(sub);
                user = userRepository.save(user);
                log.info("Linked Google identity to existing account: {}", email);
            }
        }

        // 3) Vẫn chưa có → người dùng mới hoàn toàn
        if (user == null) {
            // Pha 1: chưa gửi hồ sơ → chưa tạo tài khoản, yêu cầu frontend thu thập
            // loại sinh viên + MSSV + campus (Google không cung cấp các thông tin này).
            if (req.studentType() == null) {
                return buildProfileCompletionResponse(email, fullName);
            }
            // Pha 2: đã có hồ sơ → tạo tài khoản pending đầy đủ.
            return buildPendingResponse(createGoogleUser(sub, email, fullName, req));
        }

        // Tài khoản đã tồn tại: chặn đăng nhập nếu chưa được duyệt
        if (user.getStatus() == AccountStatus.pending) {
            return buildPendingResponse(user);
        }
        if (user.getStatus() == AccountStatus.rejected) {
            throw ApiException.forbidden("Your account has been rejected");
        }
        return buildAuthResponse(user);
    }

    /**
     * Tạo tài khoản Google mới ở trạng thái pending với hồ sơ đầy đủ (pha 2).
     * Phân giải campus/university theo loại sinh viên giống luồng đăng ký thường.
     */
    private User createGoogleUser(String sub, String email, String fullName, GoogleLoginRequest req) {
        StudentType type = req.studentType();
        Campus campus = null;
        University university = null;

        if (req.campusId() != null) {
            campus = campusRepository.findWithUniversityById(req.campusId())
                    .orElseThrow(() -> ApiException.notFound("Campus not found"));
            university = campus.getUniversity();
        } else if (req.universityId() != null) {
            university = universityRepository.findById(req.universityId())
                    .orElseThrow(() -> ApiException.notFound("University not found"));
        } else if (type == StudentType.external) {
            String universityName = req.universityName() == null ? null : req.universityName().trim();
            if (universityName == null || universityName.isBlank()) {
                throw ApiException.badRequest("University name is required for external students");
            }
            university = universityRepository.findByNameIgnoreCase(universityName)
                    .orElseGet(() -> universityRepository.save(University.builder()
                            .name(universityName)
                            .country("Vietnam")
                            .build()));
        }

        Role defaultRole = roleRepository.findByName(RoleName.TEAM_MEMBER)
                .orElseThrow(() -> new IllegalStateException("Default role team_member not seeded"));
        User created = User.builder()
                .email(email)
                .fullName(fullName)
                .googleSub(sub)
                .authProvider("google")
                .studentType(type)
                .studentId(req.studentId())
                .university(university)
                .campus(campus)
                .isGuest(false)
                .status(AccountStatus.pending)
                .termsAcceptedAt(LocalDateTime.now(ZoneOffset.UTC))
                .termsVersion(appProperties.getLegal().getTermsVersion())
                .privacyVersion(appProperties.getLegal().getPrivacyVersion())
                .roles(new HashSet<>(List.of(defaultRole)))
                .build();
        User saved = userRepository.save(created);
        log.info("Google user registered with profile (pending approval): {}", email);
        return saved;
    }

    /**
     * Đăng nhập bằng email + mật khẩu.
     * Kiểm tra mật khẩu và trạng thái tài khoản trước khi cấp token.
     *
     * @param req thông tin đăng nhập
     * @return phản hồi xác thực kèm cặp token
     * @throws ApiException nếu sai thông tin đăng nhập, hoặc tài khoản pending/rejected
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email().toLowerCase().trim())
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));

        // Dùng thông báo chung khi sai mật khẩu để tránh lộ email có tồn tại hay không
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid email or password");
        }

        if (user.getStatus() == AccountStatus.pending) {
            throw ApiException.forbidden("Your account is pending approval");
        }
        if (user.getStatus() == AccountStatus.rejected) {
            throw ApiException.forbidden("Your account has been rejected");
        }

        return buildAuthResponse(user);
    }

    /**
     * Đăng xuất: thu hồi access token và/hoặc refresh token nếu được cung cấp.
     *
     * @param authorizationHeader header Authorization dạng "Bearer &lt;token&gt;"
     * @param refreshToken        refresh token cần thu hồi (tùy chọn)
     * @throws ApiException nếu token không hợp lệ
     */
    @Transactional
    public void logout(String authorizationHeader, String refreshToken) {
        // Thu hồi access token nếu header đúng định dạng Bearer
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            try {
                jwtService.revokeToken(token);
            } catch (Exception e) {
                throw ApiException.unauthorized("Invalid access token");
            }
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                jwtService.revokeToken(refreshToken);
            } catch (Exception e) {
                throw ApiException.unauthorized("Invalid refresh token");
            }
        }
    }

    /**
     * Làm mới access token từ refresh token hợp lệ.
     * Xác minh token là loại refresh, chưa bị thu hồi, đúng security version của người dùng.
     *
     * @param req yêu cầu chứa refresh token
     * @return phản hồi xác thực với cặp token mới
     * @throws ApiException nếu token sai, không phải refresh, đã thu hồi, lỗi thời (stale),
     *                      hoặc tài khoản pending/rejected
     */
    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest req) {
        Claims claims;
        try {
            claims = jwtService.parseToken(req.refreshToken());
        } catch (Exception e) {
            throw ApiException.unauthorized("Invalid refresh token");
        }
        // Đảm bảo token đúng loại refresh, không dùng access token để refresh
        if (!jwtService.isRefreshToken(claims)) {
            throw ApiException.unauthorized("Not a refresh token");
        }
        if (jwtService.isRevoked(req.refreshToken())) {
            throw ApiException.unauthorized("Refresh token has been revoked");
        }
        Long tokenVersion = jwtService.securityVersion(claims);
        if (tokenVersion == null) {
            throw ApiException.unauthorized("Invalid refresh token security version");
        }
        UUID userId;
        try {
            userId = UUID.fromString(claims.getSubject());
        } catch (Exception e) {
            throw ApiException.unauthorized("Invalid refresh token subject");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("User not found"));
        if (user.getStatus() == AccountStatus.pending) {
            throw ApiException.forbidden("Your account is pending approval");
        }
        if (user.getStatus() == AccountStatus.rejected) {
            throw ApiException.forbidden("Your account has been rejected");
        }
        // security version lệch nghĩa là phiên bảo mật đã thay đổi (ví dụ đổi mật khẩu) => token cũ vô hiệu
        if (user.getSecurityVersion() != tokenVersion) {
            throw ApiException.unauthorized("Refresh token is stale");
        }
        return buildAuthResponse(user);
    }

    /**
     * Hoàn tất onboarding: đổi mật khẩu bắt buộc (nếu cần) và ghi nhận chấp nhận điều khoản.
     * Tăng security version để vô hiệu các token cũ.
     *
     * @param userId  ID người dùng
     * @param request dữ liệu onboarding
     * @return phản hồi xác thực đã cập nhật
     * @throws ApiException nếu không tìm thấy người dùng, thiếu mật khẩu mới khi bắt buộc đổi,
     *                      mật khẩu không khớp hoặc không đủ mạnh
     */
    @Transactional
    public AuthResponse completeOnboarding(UUID userId,
                                           vn.edu.fpt.seal.modules.auth.dto.CompleteOnboardingRequest request) {
        User user = userRepository.findWithRolesById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        // Chỉ xử lý đổi mật khẩu khi tài khoản bị đánh dấu bắt buộc đổi mật khẩu
        if (user.isMustChangePassword()) {
            if (request.newPassword() == null || request.confirmPassword() == null) {
                throw ApiException.badRequest("A new password is required");
            }
            if (!request.newPassword().equals(request.confirmPassword())) {
                throw ApiException.badRequest("Passwords do not match");
            }
            validatePassword(request.newPassword());
            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
            user.setMustChangePassword(false);
        }
        user.setTermsAcceptedAt(LocalDateTime.now(ZoneOffset.UTC));
        user.setTermsVersion(appProperties.getLegal().getTermsVersion());
        user.setPrivacyVersion(appProperties.getLegal().getPrivacyVersion());
        // Tăng security version để các token cấp trước onboarding không còn hợp lệ
        user.incrementSecurityVersion();
        return buildAuthResponse(userRepository.save(user));
    }

    /**
     * Kiểm tra độ mạnh mật khẩu: 8-72 ký tự, có cả chữ và số.
     *
     * @param password mật khẩu cần kiểm tra
     * @throws ApiException nếu mật khẩu không đạt yêu cầu
     */
    private void validatePassword(String password) {
        if (password == null || !password.matches("^(?=.*[A-Za-z])(?=.*\\d).{8,72}$")) {
            throw ApiException.badRequest("Password must be 8-72 characters and contain letters and numbers");
        }
    }

    /**
     * Xác định người dùng có cần chấp nhận lại điều khoản/chính sách hay không.
     * Trả về true nếu chưa từng chấp nhận hoặc phiên bản đã chấp nhận khác phiên bản hiện hành.
     *
     * @param user người dùng cần kiểm tra
     * @return true nếu cần chấp nhận lại điều khoản
     */
    private boolean termsAcceptanceRequired(User user) {
        return user.getTermsAcceptedAt() == null
                || !appProperties.getLegal().getTermsVersion().equals(user.getTermsVersion())
                || !appProperties.getLegal().getPrivacyVersion().equals(user.getPrivacyVersion());
    }

    /**
     * Dựng {@link AuthResponse} đầy đủ (kèm token) cho người dùng đã được duyệt.
     *
     * @param user người dùng đã approved
     * @return phản hồi xác thực đầy đủ
     * @throws ApiException nếu tài khoản chưa được duyệt
     */
    private AuthResponse buildAuthResponse(User user) {
        if (user.getStatus() != AccountStatus.approved) {
            throw ApiException.forbidden(user.getStatus() == AccountStatus.pending
                    ? "Your account is pending approval" : "Your account has been rejected");
        }
        List<String> roleNames = user.getRoles().stream().map(Role::getName).toList();
        boolean termsRequired = termsAcceptanceRequired(user);
        // onboarding cần thiết nếu phải đổi mật khẩu hoặc chấp nhận lại điều khoản
        boolean onboardingRequired = user.isMustChangePassword() || termsRequired;
        String access = jwtService.generateAccessToken(user.getId(), user.getEmail(), roleNames,
                onboardingRequired, user.getSecurityVersion());
        String refresh = jwtService.generateRefreshToken(user.getId(), user.getSecurityVersion());
        // Lấy trường từ campus nếu có, ngược lại dùng trường gán trực tiếp cho user
        Campus campus = user.getCampus();
        University university = campus != null ? campus.getUniversity() : user.getUniversity();

        return AuthResponse.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .user(AuthResponse.UserSummary.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .status(user.getStatus().name())
                        .studentType(user.getStudentType().name())
                        .universityId(university == null ? null : university.getId())
                        .universityName(university == null ? null : university.getName())
                        .campusId(campus == null ? null : campus.getId())
                        .campusName(campus == null ? null : campus.getName())
                        .isGuest(user.isGuest())
                        .roles(roleNames)
                        .mustChangePassword(user.isMustChangePassword())
                        .termsAcceptanceRequired(termsRequired)
                        .onboardingRequired(onboardingRequired)
                        .build())
                .build();
    }

    /**
     * Dựng {@link AuthResponse} cho tài khoản vừa đăng ký (pending) — không kèm token
     * vì chưa được phép đăng nhập.
     *
     * @param user người dùng ở trạng thái pending
     * @return phản hồi chỉ chứa thông tin tóm tắt
     */
    private AuthResponse buildPendingResponse(User user) {
        return AuthResponse.builder()
                .user(AuthResponse.UserSummary.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .status(user.getStatus().name())
                        .studentType(user.getStudentType().name())
                        .isGuest(user.isGuest())
                        .roles(user.getRoles().stream().map(Role::getName).toList())
                        .mustChangePassword(user.isMustChangePassword())
                        .termsAcceptanceRequired(termsAcceptanceRequired(user))
                        .onboardingRequired(false)
                        .build())
                .build();
    }

    /**
     * Dựng {@link AuthResponse} cho pha 1 đăng nhập Google: người dùng mới chưa có
     * tài khoản. Chưa tạo bản ghi, chỉ báo frontend cần thu thập thêm hồ sơ
     * (loại sinh viên, MSSV, campus) rồi gọi lại pha 2.
     *
     * @param email    email lấy từ Google (đã xác minh)
     * @param fullName tên hiển thị lấy từ Google
     * @return phản hồi mang cờ profileCompletionRequired = true, không kèm token
     */
    private AuthResponse buildProfileCompletionResponse(String email, String fullName) {
        return AuthResponse.builder()
                .user(AuthResponse.UserSummary.builder()
                        .email(email)
                        .fullName(fullName)
                        .profileCompletionRequired(true)
                        .build())
                .build();
    }
}
