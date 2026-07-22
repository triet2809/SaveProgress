package vn.edu.fpt.seal.modules.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.enums.AccountStatus;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.config.AppProperties;
import vn.edu.fpt.seal.modules.auth.entity.AccountActivationToken;
import vn.edu.fpt.seal.modules.auth.repository.AccountActivationTokenRepository;
import vn.edu.fpt.seal.modules.user.entity.User;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

/**
 * Service quản lý token kích hoạt tài khoản.
 * Chịu trách nhiệm sinh token an toàn, xác thực token, và kích hoạt tài khoản
 * (đặt mật khẩu lần đầu, chuyển trạng thái sang approved).
 */
@Service
@RequiredArgsConstructor
public class AccountActivationService {
    private final AccountActivationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    /**
     * Sinh token kích hoạt mới cho người dùng ở trạng thái pending.
     * Xóa các token chưa dùng cũ trước khi tạo token mới.
     *
     * @param user người dùng cần cấp token, phải đang ở trạng thái pending
     * @return token gốc (raw) kèm thời điểm hết hạn
     * @throws ApiException nếu tài khoản không ở trạng thái pending
     */
    @Transactional
    public IssuedToken issue(User user) {
        if (user.getStatus() != AccountStatus.pending) {
            throw ApiException.conflict("Activation tokens may only be issued for pending accounts");
        }
        // Xóa token cũ chưa dùng để tránh nhiều token hợp lệ cùng lúc
        tokenRepository.deleteActiveByUserId(user.getId());
        // Sinh 32 byte ngẫu nhiên bằng SecureRandom rồi mã hóa Base64 URL-safe làm token gốc
        byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        // Chỉ lưu hash của token vào DB, không lưu token gốc
        AccountActivationToken entity = AccountActivationToken.builder().user(user).tokenHash(hash(raw))
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(properties.getInvitation().getTokenExpirationMinutes()))
                .createdAt(LocalDateTime.now(ZoneOffset.UTC)).build();
        tokenRepository.save(entity);
        return new IssuedToken(raw, entity.getExpiresAt());
    }

    /**
     * Xác thực token kích hoạt còn hợp lệ hay không (không khóa bản ghi).
     *
     * @param raw token gốc cần kiểm tra
     * @throws ApiException nếu token không hợp lệ hoặc đã hết hạn
     */
    @Transactional(readOnly = true)
    public void validate(String raw) { findValid(raw, false); }

    /**
     * Kích hoạt tài khoản: kiểm tra mật khẩu, xác thực token, đặt mật khẩu và
     * chuyển trạng thái tài khoản sang approved, ghi nhận phiên bản điều khoản đã chấp nhận.
     *
     * @param raw      token kích hoạt gốc
     * @param password mật khẩu mới
     * @param confirm  xác nhận mật khẩu
     * @throws ApiException nếu mật khẩu không khớp, không đủ độ mạnh, token sai/hết hạn,
     *                      hoặc tài khoản không ở trạng thái pending
     */
    @Transactional
    public void activate(String raw, String password, String confirm) {
        if (!password.equals(confirm)) throw ApiException.badRequest("Passwords do not match");
        // Yêu cầu mật khẩu 8-72 ký tự, có cả chữ và số
        if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d).{8,72}$")) throw ApiException.badRequest("Password must be 8-72 characters and contain letters and numbers");
        // Khóa bản ghi token (for update) để tránh dùng token chồng chéo
        AccountActivationToken token = findValid(raw, true);
        User user = token.getUser();
        if (user.getStatus() != AccountStatus.pending) {
            throw ApiException.conflict("Activation is only available for pending accounts");
        }
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(AccountStatus.approved);
        user.setMustChangePassword(false);
        // Ghi nhận thời điểm và phiên bản điều khoản / chính sách đã chấp nhận
        user.setTermsAcceptedAt(LocalDateTime.now(ZoneOffset.UTC));
        user.setTermsVersion(properties.getLegal().getTermsVersion());
        user.setPrivacyVersion(properties.getLegal().getPrivacyVersion());
        // Đánh dấu token đã dùng để không thể tái sử dụng
        token.setUsedAt(LocalDateTime.now(ZoneOffset.UTC));
        userRepository.save(user);
        tokenRepository.save(token);
    }

    /**
     * Tìm và xác thực token hợp lệ (đúng định dạng, chưa dùng, chưa hết hạn).
     *
     * @param raw           token gốc
     * @param lockForUpdate true để khóa bản ghi (pessimistic lock) khi cần ghi
     * @return bản ghi token hợp lệ
     * @throws ApiException nếu token sai định dạng, không tồn tại, đã dùng hoặc hết hạn
     */
    private AccountActivationToken findValid(String raw, boolean lockForUpdate) {
        // Kiểm tra định dạng token cơ bản trước khi truy DB
        if (raw == null || !raw.matches("^[A-Za-z0-9_-]{32,}$")) throw ApiException.badRequest("Invalid or expired activation token");
        String tokenHash = hash(raw);
        AccountActivationToken token = (lockForUpdate
                ? tokenRepository.findByTokenHashForUpdate(tokenHash)
                : tokenRepository.findByTokenHash(tokenHash))
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired activation token"));
        // Token đã dùng hoặc đã hết hạn đều bị coi là không hợp lệ
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC)))
            throw ApiException.badRequest("Invalid or expired activation token");
        return token;
    }

    /**
     * Băm giá trị bằng SHA-256 và trả về chuỗi hex.
     *
     * @param value chuỗi cần băm
     * @return chuỗi hex của hash SHA-256
     */
    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    /**
     * Kết quả sinh token: token gốc (chỉ trả về một lần) và thời điểm hết hạn.
     *
     * @param raw       token gốc dùng để gửi cho người dùng
     * @param expiresAt thời điểm hết hạn
     */
    public record IssuedToken(String raw, LocalDateTime expiresAt) {}
}
