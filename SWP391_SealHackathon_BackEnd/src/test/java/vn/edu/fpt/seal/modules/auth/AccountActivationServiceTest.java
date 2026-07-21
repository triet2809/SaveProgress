package vn.edu.fpt.seal.modules.auth;

import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.fpt.seal.common.enums.AccountStatus;
import vn.edu.fpt.seal.config.AppProperties;
import vn.edu.fpt.seal.modules.auth.entity.AccountActivationToken;
import vn.edu.fpt.seal.modules.auth.repository.AccountActivationTokenRepository;
import vn.edu.fpt.seal.modules.auth.service.AccountActivationService;
import vn.edu.fpt.seal.modules.user.entity.User;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AccountActivationServiceTest {
    @Mock AccountActivationTokenRepository tokens; @Mock UserRepository users; @Mock PasswordEncoder encoder;
    AccountActivationService service;
    @BeforeEach void setUp() { MockitoAnnotations.openMocks(this); service = new AccountActivationService(tokens, users, encoder, new AppProperties()); when(tokens.save(any())).thenAnswer(i -> i.getArgument(0)); }

    @Test void issueStoresHashOnly() {
        User user = User.builder().email("x@example.test").fullName("X").status(AccountStatus.pending).build(); user.setId(UUID.randomUUID());
        var issued = service.issue(user);
        ArgumentCaptor<AccountActivationToken> c = ArgumentCaptor.forClass(AccountActivationToken.class); verify(tokens).save(c.capture());
        assertNotEquals(issued.raw(), c.getValue().getTokenHash()); assertEquals(64, c.getValue().getTokenHash().length());
    }

    @Test void validActivationConsumesTokenAndApproves() {
        User user = User.builder().email("x@example.test").fullName("X").status(AccountStatus.pending).build(); user.setId(UUID.randomUUID());
        var issued = service.issue(user); AccountActivationToken saved = captureToken();
        when(tokens.findByTokenHashForUpdate(saved.getTokenHash())).thenReturn(Optional.of(saved)); when(encoder.encode("Password1")).thenReturn("bcrypt");
        service.activate(issued.raw(), "Password1", "Password1");
        assertEquals(AccountStatus.approved, user.getStatus()); assertNotNull(saved.getUsedAt()); assertEquals("bcrypt", user.getPasswordHash());
        verify(tokens).findByTokenHashForUpdate(saved.getTokenHash());
    }

    @Test void reuseAndExpiredTokensRejected() {
        AccountActivationToken token = AccountActivationToken.builder().tokenHash("x").expiresAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1)).createdAt(LocalDateTime.now(ZoneOffset.UTC)).usedAt(null).build();
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        assertThrows(RuntimeException.class, () -> service.validate("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5)); token.setUsedAt(LocalDateTime.now());
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        assertThrows(RuntimeException.class, () -> service.validate("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"));
    }

    @Test void passwordMismatchRejected() {
        assertThrows(RuntimeException.class, () -> service.activate("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "Password1", "Password2"));
        verifyNoInteractions(tokens);
    }

    @Test void rejectedAccountCannotReceiveActivationToken() {
        User user = User.builder().email("rejected@example.test").fullName("Rejected")
                .status(AccountStatus.rejected).build();
        user.setId(UUID.randomUUID());

        assertThrows(RuntimeException.class, () -> service.issue(user));

        verify(tokens, never()).deleteActiveByUserId(any());
        verify(tokens, never()).save(any());
    }

    @Test void rejectedAccountCannotConsumePreviouslyIssuedToken() {
        User user = User.builder().email("rejected@example.test").fullName("Rejected")
                .status(AccountStatus.rejected).passwordHash("original").build();
        AccountActivationToken token = AccountActivationToken.builder()
                .user(user).tokenHash("hash")
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5))
                .createdAt(LocalDateTime.now(ZoneOffset.UTC)).build();
        when(tokens.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(token));

        assertThrows(RuntimeException.class, () -> service.activate(
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "Password1", "Password1"));

        assertEquals(AccountStatus.rejected, user.getStatus());
        assertEquals("original", user.getPasswordHash());
        assertNull(token.getUsedAt());
        verifyNoInteractions(encoder);
        verify(users, never()).save(any());
    }

    private AccountActivationToken captureToken() {
        ArgumentCaptor<AccountActivationToken> c = ArgumentCaptor.forClass(AccountActivationToken.class); verify(tokens).save(c.capture()); return c.getValue();
    }
}
