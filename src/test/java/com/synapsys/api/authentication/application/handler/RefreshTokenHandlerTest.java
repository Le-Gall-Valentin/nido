package com.synapsys.api.authentication.application.handler;

import com.synapsys.api.TestHashUtils;
import com.synapsys.api.authentication.domain.model.*;
import com.synapsys.api.authentication.domain.port.out.*;
import com.synapsys.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.InOrder;

@ExtendWith(MockitoExtension.class)
class RefreshTokenHandlerTest {

    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock RefreshTokenRevocationPort revocationPort;
    @Mock UserCredentialsPort userCredentialsPort;
    @Mock AccessTokenPort accessTokenPort;
    @Mock RefreshTokenIssuerPort refreshTokenPort;
    @Mock TokenHashPort tokenHashPort;
    @Mock RefreshTokenConfigPort tokenConfig;

    private RefreshTokenHandler handler;

    private final UserCredentials activeUser = new UserCredentials(
        UUID.randomUUID(), "user1", "user1@test.com", "hashed_pw", true, Role.USER
    , Instant.now());

    @BeforeEach
    void setUp() {
        when(tokenConfig.refreshTokenExpiryDays()).thenReturn(30);
        handler = new RefreshTokenHandler(
            refreshTokenRepository, revocationPort, userCredentialsPort, accessTokenPort, refreshTokenPort, tokenHashPort, tokenConfig
        );
        lenient().when(tokenHashPort.hash(any(String.class)))
            .thenAnswer(i -> TestHashUtils.sha256(i.getArgument(0, String.class)));
    }

    @Test
    void refresh_validToken_rotatesAndReturnsNewTokens() {
        String raw = "valid-raw-token";
        RefreshToken stored = new RefreshToken(
            UUID.randomUUID(), activeUser.id(), TestHashUtils.sha256(raw),
            Instant.now().plusSeconds(3600), false, Instant.now(), null
        );
        when(refreshTokenRepository.findByTokenHash(TestHashUtils.sha256(raw))).thenReturn(Optional.of(stored));
        when(userCredentialsPort.findById(activeUser.id())).thenReturn(Optional.of(activeUser));
        when(revocationPort.tryMarkUsedAndRevoke(stored.id())).thenReturn(true);
        when(accessTokenPort.generate(activeUser)).thenReturn("new_jwt");
        when(refreshTokenPort.generate(eq(activeUser), anyInt())).thenReturn("new_raw_refresh");

        AuthTokens result = handler.refresh(raw);

        assertThat(result.accessToken()).isEqualTo("new_jwt");
        assertThat(result.refreshToken()).isEqualTo("new_raw_refresh");
        InOrder inOrder = inOrder(revocationPort, refreshTokenPort);
        inOrder.verify(revocationPort).tryMarkUsedAndRevoke(stored.id());
        inOrder.verify(refreshTokenPort).generate(activeUser, 30);
    }

    @Test
    void refresh_nullToken_throwsTokenNotFound() {
        assertThatThrownBy(() -> handler.refresh(null))
            .isInstanceOf(AuthenticationException.TokenNotFound.class);
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void refresh_unknownToken_throwsTokenNotFound() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> handler.refresh("unknown"))
            .isInstanceOf(AuthenticationException.TokenNotFound.class);
    }

    @Test
    void refresh_revokedToken_revokesAllAndThrows() {
        String raw = "stolen";
        RefreshToken revoked = new RefreshToken(
            UUID.randomUUID(), activeUser.id(), TestHashUtils.sha256(raw),
            Instant.now().plusSeconds(3600), true, Instant.now(), null
        );
        when(refreshTokenRepository.findByTokenHash(TestHashUtils.sha256(raw))).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> handler.refresh(raw))
            .isInstanceOf(AuthenticationException.TokenRevoked.class);
        verify(revocationPort).revokeAllForUser(activeUser.id());
    }

    @Test
    void refresh_expiredToken_throwsTokenExpired() {
        String raw = "expired";
        RefreshToken expired = new RefreshToken(
            UUID.randomUUID(), activeUser.id(), TestHashUtils.sha256(raw),
            Instant.now().minusSeconds(1), false, Instant.now(), null
        );
        when(refreshTokenRepository.findByTokenHash(TestHashUtils.sha256(raw))).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> handler.refresh(raw))
            .isInstanceOf(AuthenticationException.TokenExpired.class);
    }

    @Test
    void refresh_revokedAndExpiredToken_revokesAllAndThrowsTokenRevoked() {
        // Revocation check takes priority: revokeAllForUser is always triggered on revoked tokens,
        // even if the token has also expired, to detect potential token theft post-TTL.
        String raw = "expired-and-revoked";
        RefreshToken expiredRevoked = new RefreshToken(
            UUID.randomUUID(), activeUser.id(), TestHashUtils.sha256(raw),
            Instant.now().minusSeconds(1), true, Instant.now(), null
        );
        when(refreshTokenRepository.findByTokenHash(TestHashUtils.sha256(raw))).thenReturn(Optional.of(expiredRevoked));

        assertThatThrownBy(() -> handler.refresh(raw))
            .isInstanceOf(AuthenticationException.TokenRevoked.class);
        verify(revocationPort).revokeAllForUser(activeUser.id());
    }

    @Test
    void refresh_deletedUser_throwsUserNotFound() {
        String raw = "valid";
        RefreshToken stored = new RefreshToken(
            UUID.randomUUID(), activeUser.id(), TestHashUtils.sha256(raw),
            Instant.now().plusSeconds(3600), false, Instant.now(), null
        );
        when(refreshTokenRepository.findByTokenHash(TestHashUtils.sha256(raw))).thenReturn(Optional.of(stored));
        when(userCredentialsPort.findById(activeUser.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.refresh(raw))
            .isInstanceOf(AuthenticationException.UserNotFound.class);
    }

    @Test
    void refresh_concurrentReuse_throwsTokenRevoked() {
        String raw = "valid-concurrent";
        RefreshToken stored = new RefreshToken(
            UUID.randomUUID(), activeUser.id(), TestHashUtils.sha256(raw),
            Instant.now().plusSeconds(3600), false, Instant.now(), null
        );
        when(refreshTokenRepository.findByTokenHash(TestHashUtils.sha256(raw))).thenReturn(Optional.of(stored));
        when(userCredentialsPort.findById(activeUser.id())).thenReturn(Optional.of(activeUser));
        when(revocationPort.tryMarkUsedAndRevoke(stored.id())).thenReturn(false);

        assertThatThrownBy(() -> handler.refresh(raw))
            .isInstanceOf(AuthenticationException.TokenRevoked.class);
        verifyNoInteractions(refreshTokenPort);
    }

    @Test
    void refresh_revokedToken_revokeAllForUserFails_stillThrowsTokenRevoked() {
        String raw = "stolen";
        RefreshToken revoked = new RefreshToken(
            UUID.randomUUID(), activeUser.id(), TestHashUtils.sha256(raw),
            Instant.now().plusSeconds(3600), true, Instant.now(), null
        );
        when(refreshTokenRepository.findByTokenHash(TestHashUtils.sha256(raw))).thenReturn(Optional.of(revoked));
        doThrow(new RuntimeException("DB down")).when(revocationPort).revokeAllForUser(activeUser.id());

        assertThatThrownBy(() -> handler.refresh(raw))
            .isInstanceOf(AuthenticationException.TokenRevoked.class);
    }

    @Test
    void refresh_generateThrows_propagatesException() {
        String raw = "valid-raw-token";
        RefreshToken stored = new RefreshToken(
            UUID.randomUUID(), activeUser.id(), TestHashUtils.sha256(raw),
            Instant.now().plusSeconds(3600), false, Instant.now(), null
        );
        when(refreshTokenRepository.findByTokenHash(TestHashUtils.sha256(raw))).thenReturn(Optional.of(stored));
        when(userCredentialsPort.findById(activeUser.id())).thenReturn(Optional.of(activeUser));
        when(revocationPort.tryMarkUsedAndRevoke(stored.id())).thenReturn(true);
        when(accessTokenPort.generate(activeUser)).thenReturn("new_jwt");
        when(refreshTokenPort.generate(eq(activeUser), anyInt()))
            .thenThrow(new RuntimeException("token store unavailable"));

        assertThatThrownBy(() -> handler.refresh(raw))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("token store unavailable");
    }

    @Test
    void refresh_inactiveUser_throwsUserNotActive() {
        String raw = "valid-raw-token";
        UserCredentials inactiveUser = new UserCredentials(
            activeUser.id(), activeUser.username(), activeUser.email(),
            activeUser.passwordHash(), false, activeUser.role()
        , Instant.now());
        RefreshToken stored = new RefreshToken(
            UUID.randomUUID(), inactiveUser.id(), TestHashUtils.sha256(raw),
            Instant.now().plusSeconds(3600), false, Instant.now(), null
        );
        when(refreshTokenRepository.findByTokenHash(TestHashUtils.sha256(raw))).thenReturn(Optional.of(stored));
        when(userCredentialsPort.findById(inactiveUser.id())).thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> handler.refresh(raw))
            .isInstanceOf(AuthenticationException.UserNotActive.class);
        verifyNoInteractions(refreshTokenPort);
    }
}