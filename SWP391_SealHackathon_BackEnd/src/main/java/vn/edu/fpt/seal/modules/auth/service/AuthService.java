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
import vn.edu.fpt.seal.config.AppProperties;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

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
    private final AppProperties appProperties;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email().toLowerCase().trim())) {
            throw ApiException.conflict("Email already registered");
        }

        StudentType type = req.studentType() != null ? req.studentType() : StudentType.none;
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

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email().toLowerCase().trim())
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));

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

    @Transactional
    public void logout(String authorizationHeader, String refreshToken) {
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

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest req) {
        Claims claims;
        try {
            claims = jwtService.parseToken(req.refreshToken());
        } catch (Exception e) {
            throw ApiException.unauthorized("Invalid refresh token");
        }
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
        if (user.getSecurityVersion() != tokenVersion) {
            throw ApiException.unauthorized("Refresh token is stale");
        }
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse completeOnboarding(UUID userId,
                                           vn.edu.fpt.seal.modules.auth.dto.CompleteOnboardingRequest request) {
        User user = userRepository.findWithRolesById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
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
        user.incrementSecurityVersion();
        return buildAuthResponse(userRepository.save(user));
    }

    private void validatePassword(String password) {
        if (password == null || !password.matches("^(?=.*[A-Za-z])(?=.*\\d).{8,72}$")) {
            throw ApiException.badRequest("Password must be 8-72 characters and contain letters and numbers");
        }
    }

    private boolean termsAcceptanceRequired(User user) {
        return user.getTermsAcceptedAt() == null
                || !appProperties.getLegal().getTermsVersion().equals(user.getTermsVersion())
                || !appProperties.getLegal().getPrivacyVersion().equals(user.getPrivacyVersion());
    }

    private AuthResponse buildAuthResponse(User user) {
        if (user.getStatus() != AccountStatus.approved) {
            throw ApiException.forbidden(user.getStatus() == AccountStatus.pending
                    ? "Your account is pending approval" : "Your account has been rejected");
        }
        List<String> roleNames = user.getRoles().stream().map(Role::getName).toList();
        boolean termsRequired = termsAcceptanceRequired(user);
        boolean onboardingRequired = user.isMustChangePassword() || termsRequired;
        String access = jwtService.generateAccessToken(user.getId(), user.getEmail(), roleNames,
                onboardingRequired, user.getSecurityVersion());
        String refresh = jwtService.generateRefreshToken(user.getId(), user.getSecurityVersion());
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
}
