package vn.edu.fpt.seal.modules.auth;

import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.fpt.seal.common.enums.*;
import vn.edu.fpt.seal.config.AppProperties;
import vn.edu.fpt.seal.modules.auth.dto.*;
import vn.edu.fpt.seal.modules.auth.service.AuthService;
import vn.edu.fpt.seal.modules.university.repository.CampusRepository;
import vn.edu.fpt.seal.modules.university.repository.UniversityRepository;
import vn.edu.fpt.seal.modules.university.entity.Campus;
import vn.edu.fpt.seal.modules.university.entity.University;
import vn.edu.fpt.seal.modules.user.entity.Role;
import vn.edu.fpt.seal.modules.user.entity.User;
import vn.edu.fpt.seal.modules.user.repository.RoleRepository;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.security.JwtService;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthOnboardingServiceTest {
    @Mock UserRepository users;
    @Mock RoleRepository roles;
    @Mock CampusRepository campuses;
    @Mock UniversityRepository universities;
    @Mock PasswordEncoder encoder;
    @Mock JwtService jwt;

    AppProperties properties;
    AuthService service;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        properties.getLegal().setTermsVersion("terms-v2");
        properties.getLegal().setPrivacyVersion("privacy-v2");
        service = new AuthService(users, roles, campuses, universities, encoder, jwt, properties);
        lenient().when(jwt.generateAccessToken(any(), anyString(), anyList(), anyBoolean())).thenReturn("access");
        lenient().when(jwt.generateAccessToken(any(), anyString(), anyList(), anyBoolean(), anyLong())).thenReturn("access");
        lenient().when(jwt.generateRefreshToken(any())).thenReturn("refresh");
        lenient().when(jwt.generateRefreshToken(any(), anyLong())).thenReturn("refresh");
    }

    @Test
    void temporaryPasswordLoginReturnsRestrictedOnboardingSession() {
        User user = staffUser();
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(encoder.matches("Temporary123", user.getPasswordHash())).thenReturn(true);

        AuthResponse response = service.login(new LoginRequest(user.getEmail(), "Temporary123"));

        assertTrue(response.user().mustChangePassword());
        assertTrue(response.user().termsAcceptanceRequired());
        assertTrue(response.user().onboardingRequired());
        verify(jwt).generateAccessToken(user.getId(), user.getEmail(), List.of("judge"), true, user.getSecurityVersion());
    }

    @Test
    void onboardingChangesPasswordAndRecordsCurrentLegalVersions() {
        User user = staffUser();
        when(users.findWithRolesById(user.getId())).thenReturn(Optional.of(user));
        when(encoder.encode("Permanent123")).thenReturn("new-hash");
        when(users.save(user)).thenReturn(user);

        AuthResponse response = service.completeOnboarding(user.getId(),
                new CompleteOnboardingRequest("Permanent123", "Permanent123", true));

        assertFalse(user.isMustChangePassword());
        assertEquals("new-hash", user.getPasswordHash());
        assertNotNull(user.getTermsAcceptedAt());
        assertEquals("terms-v2", user.getTermsVersion());
        assertEquals("privacy-v2", user.getPrivacyVersion());
        assertFalse(response.user().onboardingRequired());
    }

    @Test
    void legalVersionChangeRequiresAcceptanceAgainWithoutForcingPasswordChange() {
        User user = staffUser();
        user.setMustChangePassword(false);
        user.setTermsAcceptedAt(LocalDateTime.now());
        user.setTermsVersion("terms-v1");
        user.setPrivacyVersion("privacy-v1");
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(encoder.matches("Password123", user.getPasswordHash())).thenReturn(true);

        AuthResponse response = service.login(new LoginRequest(user.getEmail(), "Password123"));

        assertFalse(response.user().mustChangePassword());
        assertTrue(response.user().termsAcceptanceRequired());
        assertTrue(response.user().onboardingRequired());
    }

    @Test
    void registrationPersistsCurrentLegalAcceptance() {
        Role role = Role.builder().name("team_member").build();
        when(roles.findByName(RoleName.TEAM_MEMBER)).thenReturn(Optional.of(role));
        when(encoder.encode("Password123")).thenReturn("hash");
        when(users.save(any())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        AuthResponse response = service.register(new RegisterRequest("new@example.test", "Password123", "New User",
                StudentType.none, null, null, null, null, true));

        assertNull(response.accessToken());
        assertNull(response.refreshToken());
        assertEquals("pending", response.user().status());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        assertNotNull(captor.getValue().getTermsAcceptedAt());
        assertEquals("terms-v2", captor.getValue().getTermsVersion());
        assertEquals("privacy-v2", captor.getValue().getPrivacyVersion());
    }

    @Test
    void registrationPersistsSelectedCampusAndItsUniversity() {
        UUID campusId = UUID.randomUUID();
        University university = University.builder().name("FPT University").build();
        university.setId(UUID.randomUUID());
        Campus campus = Campus.builder().university(university).name("FPT University Quy Nhon")
                .city("Quy Nhon").build();
        campus.setId(campusId);
        Role role = Role.builder().name("team_member").build();
        when(campuses.findWithUniversityById(campusId)).thenReturn(Optional.of(campus));
        when(roles.findByName(RoleName.TEAM_MEMBER)).thenReturn(Optional.of(role));
        when(encoder.encode("Password123")).thenReturn("hash");
        when(users.save(any())).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        AuthResponse response = service.register(new RegisterRequest("quynhon@example.test", "Password123",
                "Quy Nhon Student", StudentType.fpt, "SE123", null, null, campusId, true));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        assertSame(campus, captor.getValue().getCampus());
        assertSame(university, captor.getValue().getUniversity());
        assertEquals(campusId, response.user().campusId());
        assertEquals("FPT University Quy Nhon", response.user().campusName());
    }

    @Test
    void pendingLoginIsRejected() {
        User user = staffUser();
        user.setStatus(AccountStatus.pending);
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(encoder.matches("Password123", user.getPasswordHash())).thenReturn(true);

        assertThrows(vn.edu.fpt.seal.common.exception.ApiException.class,
                () -> service.login(new LoginRequest(user.getEmail(), "Password123")));
        verifyNoInteractions(jwt);
    }

    @Test
    void rejectedLoginIsRejected() {
        User user = staffUser();
        user.setStatus(AccountStatus.rejected);
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(encoder.matches("Password123", user.getPasswordHash())).thenReturn(true);

        assertThrows(vn.edu.fpt.seal.common.exception.ApiException.class,
                () -> service.login(new LoginRequest(user.getEmail(), "Password123")));
        verifyNoInteractions(jwt);
    }

    @Test
    void staleRefreshIsRejectedAfterOnboarding() {
        User user = staffUser();
        when(jwt.parseToken("refresh")).thenReturn(org.mockito.Mockito.mock(io.jsonwebtoken.Claims.class));
        var claims = org.mockito.Mockito.mock(io.jsonwebtoken.Claims.class);
        when(jwt.parseToken("refresh")).thenReturn(claims);
        when(jwt.isRefreshToken(claims)).thenReturn(true);
        when(jwt.isRevoked("refresh")).thenReturn(false);
        when(jwt.securityVersion(claims)).thenReturn(1L);
        when(claims.getSubject()).thenReturn(user.getId().toString());
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(users.findWithRolesById(user.getId())).thenReturn(Optional.of(user));
        when(encoder.encode("Permanent123")).thenReturn("new-hash");
        when(users.save(user)).thenReturn(user);

        service.completeOnboarding(user.getId(),
                new CompleteOnboardingRequest("Permanent123", "Permanent123", true));

        assertThrows(vn.edu.fpt.seal.common.exception.ApiException.class,
                () -> service.refresh(new RefreshRequest("refresh")));
    }

    @Test
    void pendingRefreshIsRejected() {
        User user = staffUser();
        user.setStatus(AccountStatus.pending);
        var claims = org.mockito.Mockito.mock(io.jsonwebtoken.Claims.class);
        when(jwt.parseToken("refresh")).thenReturn(claims);
        when(jwt.isRefreshToken(claims)).thenReturn(true);
        when(jwt.isRevoked("refresh")).thenReturn(false);
        when(jwt.securityVersion(claims)).thenReturn(user.getSecurityVersion());
        when(claims.getSubject()).thenReturn(user.getId().toString());
        when(users.findById(user.getId())).thenReturn(Optional.of(user));

        assertThrows(vn.edu.fpt.seal.common.exception.ApiException.class,
                () -> service.refresh(new RefreshRequest("refresh")));
    }

    @Test
    void rejectedRefreshIsRejected() {
        User user = staffUser();
        user.setStatus(AccountStatus.rejected);
        var claims = org.mockito.Mockito.mock(io.jsonwebtoken.Claims.class);
        when(jwt.parseToken("refresh")).thenReturn(claims);
        when(jwt.isRefreshToken(claims)).thenReturn(true);
        when(jwt.isRevoked("refresh")).thenReturn(false);
        when(jwt.securityVersion(claims)).thenReturn(user.getSecurityVersion());
        when(claims.getSubject()).thenReturn(user.getId().toString());
        when(users.findById(user.getId())).thenReturn(Optional.of(user));

        assertThrows(vn.edu.fpt.seal.common.exception.ApiException.class,
                () -> service.refresh(new RefreshRequest("refresh")));
    }

    @Test
    void malformedRefreshVersionClaimIsRejected() {
        var claims = org.mockito.Mockito.mock(io.jsonwebtoken.Claims.class);
        when(jwt.parseToken("refresh")).thenReturn(claims);
        when(jwt.isRefreshToken(claims)).thenReturn(true);
        when(jwt.isRevoked("refresh")).thenReturn(false);
        when(jwt.securityVersion(claims)).thenReturn(null);

        assertThrows(vn.edu.fpt.seal.common.exception.ApiException.class,
                () -> service.refresh(new RefreshRequest("refresh")));
        verifyNoInteractions(users);
    }

    @Test
    void registrationAndOnboardingRejectMissingAcceptanceAtValidationBoundary() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertFalse(validator.validate(new RegisterRequest("new@example.test", "Password123", "New User",
                    StudentType.none, null, null, null, null, false)).isEmpty());
            assertFalse(validator.validate(new CompleteOnboardingRequest("Password123", "Password123", null)).isEmpty());
        }
    }

    private User staffUser() {
        Role judge = Role.builder().name("judge").build();
        User user = User.builder().email("judge@example.test").fullName("Judge")
                .passwordHash("hash").status(AccountStatus.approved).mustChangePassword(true)
                .studentType(StudentType.none).roles(new HashSet<>(Set.of(judge))).build();
        user.setId(UUID.randomUUID());
        return user;
    }
}
