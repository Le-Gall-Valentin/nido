package com.synapsys.api.authentication.application.handler;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.synapsys.api.authentication.application.dto.LoginCommand;
import com.synapsys.api.authentication.domain.model.*;
import com.synapsys.api.authentication.domain.port.out.*;
import com.synapsys.api.shared.model.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginHandlerTest {

    @Mock UserCredentialsPort userCredentialsPort;
    @Mock PasswordHasherPort passwordHasher;
    @Mock PasswordVerifierPort passwordVerifier;
    @Mock AccessTokenPort accessTokenPort;
    @Mock RefreshTokenIssuerPort refreshTokenPort;
    @Mock RefreshTokenConfigPort tokenConfig;
    @Mock TotpChallengeStorePort totpChallengeStore;
    @Mock TotpStatusQueryPort totpStatusQuery;

    private LoginHandler handler;
    private ListAppender<ILoggingEvent> logAppender;

    private final UserCredentials activeUser = new UserCredentials(
        UUID.randomUUID(), "user1", "user1@test.com", "hashed_pw", true, Role.USER
    , Instant.now());

    private final UserCredentials activeUser2 = new UserCredentials(
        UUID.randomUUID(), "user2", "user2@test.com", "hashed_pw", true, Role.USER
    , Instant.now());

    @BeforeEach
    void setUpLogger() {
        logAppender = new ListAppender<>();
        logAppender.start();
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(LoginHandler.class))
            .addAppender(logAppender);
    }

    @AfterEach
    void tearDownLogger() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(LoginHandler.class))
            .detachAppender(logAppender);
    }

    @BeforeEach
    void setUp() {
        // Constructor calls passwordHasher.hash() to precompute the dummy hash — stub it first
        lenient().when(passwordHasher.hash(anyString())).thenReturn("$2a$12$stubbed-dummy-hash-for-tests");
        when(tokenConfig.refreshTokenExpiryDays()).thenReturn(30);
        handler = new LoginHandler(userCredentialsPort, passwordHasher, passwordVerifier, accessTokenPort, refreshTokenPort, tokenConfig, totpChallengeStore, totpStatusQuery);
    }

    @Test
    void login_success_returnsTokensAndUser() {
        when(userCredentialsPort.findByUsername("user1")).thenReturn(Optional.of(activeUser));
        when(passwordVerifier.matches("password", "hashed_pw")).thenReturn(true);
        when(accessTokenPort.generate(activeUser)).thenReturn("jwt_access");
        when(refreshTokenPort.generate(eq(activeUser), anyInt())).thenReturn("raw_refresh");

        LoginResult result = handler.login(new LoginCommand("user1", "password"));

        assertThat(result).isInstanceOf(LoginResult.Success.class);
        LoginResult.Success success = (LoginResult.Success) result;
        assertThat(success.tokens().accessToken()).isEqualTo("jwt_access");
        assertThat(success.tokens().refreshToken()).isEqualTo("raw_refresh");
        assertThat(success.credentials()).isEqualTo(activeUser);
        verify(refreshTokenPort).generate(activeUser, 30);
    }

    @Test
    void login_unknownUser_throwsInvalidCredentials() {
        when(userCredentialsPort.findByUsername("user1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.login(new LoginCommand("user1", "password")))
            .isInstanceOf(AuthenticationException.InvalidCredentials.class);
        verifyNoInteractions(refreshTokenPort);
    }

    @Test
    void login_unknownUser_performsDummyHashComparison() {
        when(userCredentialsPort.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.login(new LoginCommand("unknown", "password")))
            .isInstanceOf(AuthenticationException.InvalidCredentials.class);

        // Dummy comparison must be performed to prevent timing-based username enumeration
        verify(passwordVerifier).matches(eq("password"), anyString());
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        when(userCredentialsPort.findByUsername("user1")).thenReturn(Optional.of(activeUser));
        when(passwordVerifier.matches("wrong", "hashed_pw")).thenReturn(false);

        assertThatThrownBy(() -> handler.login(new LoginCommand("user1", "wrong")))
            .isInstanceOf(AuthenticationException.InvalidCredentials.class);
    }

    @Test
    void login_inactiveUser_throwsUserNotActive() {
        UserCredentials inactive = new UserCredentials(activeUser.id(), activeUser.username(), activeUser.email(),
            activeUser.passwordHash(), false, activeUser.role(), Instant.now());
        when(userCredentialsPort.findByUsername("user1")).thenReturn(Optional.of(inactive));
        when(passwordVerifier.matches("password", inactive.passwordHash())).thenReturn(true);

        assertThatThrownBy(() -> handler.login(new LoginCommand("user1", "password")))
            .isInstanceOf(AuthenticationException.UserNotActive.class);
    }

    @Test
    void login_inactiveUser_doesNotLogUsername() {
        UserCredentials inactive = new UserCredentials(activeUser.id(), "alice", activeUser.email(),
            activeUser.passwordHash(), false, activeUser.role(), Instant.now());
        when(userCredentialsPort.findByUsername("alice")).thenReturn(Optional.of(inactive));
        when(passwordVerifier.matches("password", inactive.passwordHash())).thenReturn(true);

        assertThatThrownBy(() -> handler.login(new LoginCommand("alice", "password")))
            .isInstanceOf(AuthenticationException.UserNotActive.class);

        boolean usernameLogged = logAppender.list.stream()
            .anyMatch(e -> e.getFormattedMessage().contains("alice"));
        assertThat(usernameLogged).isFalse();
    }

    @Test
    void login_wrongPassword_doesNotLogUsername() {
        when(userCredentialsPort.findByUsername("bob")).thenReturn(Optional.of(
            new UserCredentials(activeUser.id(), "bob", activeUser.email(),
                activeUser.passwordHash(), true, activeUser.role(), Instant.now())
        ));
        when(passwordVerifier.matches("wrong", activeUser.passwordHash())).thenReturn(false);

        assertThatThrownBy(() -> handler.login(new LoginCommand("bob", "wrong")))
            .isInstanceOf(AuthenticationException.InvalidCredentials.class);

        boolean usernameLogged = logAppender.list.stream()
            .anyMatch(e -> e.getFormattedMessage().contains("bob"));
        assertThat(usernameLogged).isFalse();
    }

    @Test
    void login_inactiveUser_correctPassword_throwsUserNotActive() {
        UserCredentials inactive = new UserCredentials(activeUser.id(), activeUser.username(), activeUser.email(),
            activeUser.passwordHash(), false, activeUser.role(), Instant.now());
        when(userCredentialsPort.findByUsername("user1")).thenReturn(Optional.of(inactive));
        when(passwordVerifier.matches("correctpassword", inactive.passwordHash())).thenReturn(true);

        assertThatThrownBy(() -> handler.login(new LoginCommand("user1", "correctpassword")))
            .isInstanceOf(AuthenticationException.UserNotActive.class);
    }

    @Test
    void login_inactiveUser_wrongPassword_throwsInvalidCredentials() {
        // Password is checked before isActive to prevent account status info disclosure.
        // An attacker who doesn't know the password must not learn the account exists and is inactive.
        UserCredentials inactive = new UserCredentials(activeUser.id(), activeUser.username(), activeUser.email(),
            activeUser.passwordHash(), false, activeUser.role(), Instant.now());
        when(userCredentialsPort.findByUsername("user1")).thenReturn(Optional.of(inactive));
        when(passwordVerifier.matches("wrongpassword", inactive.passwordHash())).thenReturn(false);

        assertThatThrownBy(() -> handler.login(new LoginCommand("user1", "wrongpassword")))
            .isInstanceOf(AuthenticationException.InvalidCredentials.class);
    }

    @Test
    void login_inactiveUser_totpEnabled_throwsUserNotActive() {
        UserCredentials inactiveTotpUser = new UserCredentials(
            UUID.randomUUID(), "user2", "user2@test.com", "hashed_pw", false, Role.USER, Instant.now());
        when(userCredentialsPort.findByUsername("user2")).thenReturn(Optional.of(inactiveTotpUser));
        when(passwordVerifier.matches("password", "hashed_pw")).thenReturn(true);

        assertThatThrownBy(() -> handler.login(new LoginCommand("user2", "password")))
            .isInstanceOf(AuthenticationException.UserNotActive.class);
        verifyNoInteractions(totpChallengeStore, accessTokenPort, refreshTokenPort, totpStatusQuery);
    }

    @Test
    void login_totpStatusQuery_notCalledOnUnknownUser() {
        when(userCredentialsPort.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.login(new LoginCommand("unknown", "password")))
            .isInstanceOf(AuthenticationException.InvalidCredentials.class);

        verifyNoInteractions(totpStatusQuery);
    }

    @Test
    void login_totpStatusQuery_notCalledOnWrongPassword() {
        when(userCredentialsPort.findByUsername("user1")).thenReturn(Optional.of(activeUser));
        when(passwordVerifier.matches("wrong", "hashed_pw")).thenReturn(false);

        assertThatThrownBy(() -> handler.login(new LoginCommand("user1", "wrong")))
            .isInstanceOf(AuthenticationException.InvalidCredentials.class);

        verifyNoInteractions(totpStatusQuery);
    }

    @Test
    void login_success_accessTokenThrows_propagatesException() {
        when(userCredentialsPort.findByUsername("user1")).thenReturn(Optional.of(activeUser));
        when(passwordVerifier.matches("password", "hashed_pw")).thenReturn(true);
        when(accessTokenPort.generate(activeUser)).thenThrow(new RuntimeException("jwt store down"));

        assertThatThrownBy(() -> handler.login(new LoginCommand("user1", "password")))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("jwt store down");
        verifyNoInteractions(refreshTokenPort);
    }

    @Test
    void login_success_refreshTokenThrows_propagatesException() {
        when(userCredentialsPort.findByUsername("user1")).thenReturn(Optional.of(activeUser));
        when(passwordVerifier.matches("password", "hashed_pw")).thenReturn(true);
        when(accessTokenPort.generate(activeUser)).thenReturn("jwt_access");
        when(refreshTokenPort.generate(eq(activeUser), anyInt()))
            .thenThrow(new RuntimeException("refresh store down"));

        assertThatThrownBy(() -> handler.login(new LoginCommand("user1", "password")))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("refresh store down");
    }

    @Test
    void login_totpEnabled_returnsTotpRequired_andStoresChallenge() {
        when(userCredentialsPort.findByUsername("user2")).thenReturn(Optional.of(activeUser2));
        when(passwordVerifier.matches("password", "hashed_pw")).thenReturn(true);
        when(totpStatusQuery.isTotpEnabled(activeUser2.id())).thenReturn(true);
        when(totpChallengeStore.createChallenge(activeUser2.id())).thenReturn("challenge-uuid");

        LoginResult result = handler.login(new LoginCommand("user2", "password"));

        assertThat(result).isInstanceOf(LoginResult.TotpRequired.class);
        assertThat(((LoginResult.TotpRequired) result).challengeId()).isEqualTo("challenge-uuid");
        verifyNoInteractions(accessTokenPort, refreshTokenPort);
    }

    @Test
    void login_totpEnabled_doesNotIssueTokens() {
        when(userCredentialsPort.findByUsername("user2")).thenReturn(Optional.of(activeUser2));
        when(passwordVerifier.matches("password", "hashed_pw")).thenReturn(true);
        when(totpStatusQuery.isTotpEnabled(activeUser2.id())).thenReturn(true);
        when(totpChallengeStore.createChallenge(activeUser2.id())).thenReturn("challenge-uuid");

        handler.login(new LoginCommand("user2", "password"));

        verifyNoInteractions(accessTokenPort);
        verifyNoInteractions(refreshTokenPort);
    }
}
