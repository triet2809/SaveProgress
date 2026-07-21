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

@Service
@RequiredArgsConstructor
public class AccountActivationService {
    private final AccountActivationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    @Transactional
    public IssuedToken issue(User user) {
        if (user.getStatus() != AccountStatus.pending) {
            throw ApiException.conflict("Activation tokens may only be issued for pending accounts");
        }
        tokenRepository.deleteActiveByUserId(user.getId());
        byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        AccountActivationToken entity = AccountActivationToken.builder().user(user).tokenHash(hash(raw))
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(properties.getInvitation().getTokenExpirationMinutes()))
                .createdAt(LocalDateTime.now(ZoneOffset.UTC)).build();
        tokenRepository.save(entity);
        return new IssuedToken(raw, entity.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public void validate(String raw) { findValid(raw, false); }

    @Transactional
    public void activate(String raw, String password, String confirm) {
        if (!password.equals(confirm)) throw ApiException.badRequest("Passwords do not match");
        if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d).{8,72}$")) throw ApiException.badRequest("Password must be 8-72 characters and contain letters and numbers");
        AccountActivationToken token = findValid(raw, true);
        User user = token.getUser();
        if (user.getStatus() != AccountStatus.pending) {
            throw ApiException.conflict("Activation is only available for pending accounts");
        }
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(AccountStatus.approved);
        user.setMustChangePassword(false);
        user.setTermsAcceptedAt(LocalDateTime.now(ZoneOffset.UTC));
        user.setTermsVersion(properties.getLegal().getTermsVersion());
        user.setPrivacyVersion(properties.getLegal().getPrivacyVersion());
        token.setUsedAt(LocalDateTime.now(ZoneOffset.UTC));
        userRepository.save(user);
        tokenRepository.save(token);
    }

    private AccountActivationToken findValid(String raw, boolean lockForUpdate) {
        if (raw == null || !raw.matches("^[A-Za-z0-9_-]{32,}$")) throw ApiException.badRequest("Invalid or expired activation token");
        String tokenHash = hash(raw);
        AccountActivationToken token = (lockForUpdate
                ? tokenRepository.findByTokenHashForUpdate(tokenHash)
                : tokenRepository.findByTokenHash(tokenHash))
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired activation token"));
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC)))
            throw ApiException.badRequest("Invalid or expired activation token");
        return token;
    }
    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public record IssuedToken(String raw, LocalDateTime expiresAt) {}
}
