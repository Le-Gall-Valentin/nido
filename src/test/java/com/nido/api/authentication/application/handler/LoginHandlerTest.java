package com.nido.api.authentication.application.handler;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.nido.api.authentication.application.dto.LoginCommand;
import com.nido.api.authentication.domain.model.AuthenticationException;
import com.nido.api.authentication.domain.model.LoginResult;
import com.nido.api.authentication.domain.model.UserCredentials;
import com.nido.api.authentication.domain.port.out.*;
import com.nido.api.authentication.domain.model.*;
import com.nido.api.authentication.domain.port.out.*;
import com.nido.api.shared.model.Role;
import com.nido.api.shared.model.TwoFactorMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginHandlerTest {

    @Mock UserCredentialsPort userCredentialsPort;
    @Mock
    PasswordHasherPort passwordHasher;
    @Mock
    PasswordVerifierPort passwordVerifier;
    @Mock
    AccessTokenPort accessTokenPort;
    @Mock
    RefreshTokenIssuerPort refreshTokenPort;
    @Mock RefreshTokenConfigPort tokenConfig;
    @Mock
    TwoFactorChallengeStorePort challengeStore;
    @Mock TwoFactorChallengePort secondFactor;

    private LoginHandler handler;
    private ListAppender<ILoggingEvent> logAppender;

    private final UserCredentials activeUser = new UserCredentials(
        UUID.randomUUID(), "user1", "user1@test.com", "hashed_pw", true, Role.USER
    , Instant.now(), null);

    private final UserCredentials activeUser2 = new UserCredentials(
        UUID.randomUUID(), "user2", "user2@test.com", "hashed_pw", true, Role.USER
    , Instant.now(), null);

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
        lenient().when(tokenConfig.refreshTokenExpiryDays()).thenReturn(30);
        handler = new LoginHandler(userCredentialsPort, passwordHasher, passwordVerifier, accessTokenPort, refreshTokenPort, tokenConfig, challengeStore, secondFactor);
    }

    @Test
    void login_success_returnsTokensAndUser() {
        when(userCredentialsPort.findByIdentifier("user1")).thenReturn(Optional.of(activeUser));
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
        when(userCredentialsPort.findByIdentifier("user1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.login(new LoginCommand("user1", "password")))
            .isInstanceOf(AuthenticationException.InvalidCredentials.class);
        verifyNoInteractions(refreshTokenPort);
    }

    @Test
    void login_unknownUser_performsDummyHashComparison() {
        when(userCredentialsPort.findByIdentifier("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.login(new LoginCommand("unknown", "password")))
            .isInstanceOf(AuthenticationException.InvalidCredentials.class);

        // Dummy comparison must be performed to prevent timing-based username enumeration
        verify(passwordVerifier).matches(eq("password"), anyString());
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        when(userCredentialsPort.findByIdentifier("user1")).thenReturn(Optional.of(activeUser));
        when(passwordVerifier.matches("wrong", "hashed_pw")).thenReturn(false);

        assertThatThrownBy(() -> handler.login(new LoginCommand("user1", "wrong")))
            .isInstanceOf(AuthenticationException.InvalidCredentials.class);
    }

    @Test
    void login_inactiveUser_throwsUserNotActive() {
        UserCredentials inactive = new UserCredentials(activeUser.id(), activeUser.username(), activeUser.email(),
            activeUser.passwordHash(), false, activeUser.role(), Instant.now(), null);
        when(userCredentialsPort.findByIdentifier("user1")).thenReturn(Optional.of(inactive));
        when(passwordVerifier.matches("password", inactive.passwordHash())).thenReturn(true);

        assertThatThrownBy(() -> handler.login(new LoginCommand("user1", "password")))
            .isInstanceOf(AuthenticationException.UserNotActive.class);
    }

    @Test
    void login_inactiveUser_doesNotLogUsername() {
        UserCredentials inactive = new UserCredentials(activeUser.id(), "alice", activeUser.email(),
            activeUser.passwordHash(), false, activeUser.role(), Instant.now(), null);
        when(userCredentialsPort.findByIdentifier("alice")).thenReturn(Optional.of(inactive));
        when(passwordVerifier.matches("password", inactive.passwordHash())).thenReturn(true);

        assertThatThrownBy(() -> handler.login(new LoginCommand("alice", "password")))
            .isInstanceOf(AuthenticationException.UserNotActive.class);

        boolean usernameLogged = logAppender.list.stream()
            .anyMatch(e -> e.getFormattedMessage().contains("alice"));
        assertThat(usernameLogged).isFalse();
    }

    @Test
    void login_wrongPassword_doesNotLogUsername() {
        when(userCredentialsPort.findByIdentifier("bob")).thenReturn(Optional.of(
            new UserCredentials(activeUser.id(), "bob", activeUser.email(),
                activeUser.passwordHash(), true, activeUser.role(), Instant.now(), null)
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
            activeUser.passwordHash(), false, activeUser.role(), Instant.now(), null);
        when(userCredentialsPort.findByIdentifier("user1")).thenReturn(Optional.of(inactive));
        when(passwordVerifier.matches("correctpassword", inactive.passwordHash())).thenReturn(true);

        assertThatThrownBy(() -> handler.login(new LoginCommand("user1", "correctpassword")))
            .isInstanceOf(AuthenticationException.UserNotActive.class);
    }

    @Test
    void login_inactiveUser_wrongPassword_throwsInvalidCredentials() {
        // Password is checked before isActive to prevent account status info disclosure.
        // An attacker who doesn't know the password must not learn the account exists and is inactive.
        UserCredentials inactive = new UserCredentials(activeUser.id(), activeUser.username(), activeUser.email(),
            activeUser.passwordHash(), false, activeUser.role(), Instant.now(), null);
        when(userCredentialsPort.findByIdentifier("user1")).thenReturn(Optional.of(inactive));
        when(passwordVerifier.matches("wrongpassword", inactive.passwordHash())).thenReturn(false);

        assertThatThrownBy(() -> handler.login(new LoginCommand("user1", "wrongpassword")))
            .isInstanceOf(AuthenticationException.InvalidCredentials.class);
    }

    @Test
    void login_inactiveUser_withTwoFactorOn_throwsUserNotActive() {
        UserCredentials inactiveTotpUser = new UserCredentials(
            UUID.randomUUID(), "user2", "user2@test.com", "hashed_pw", false, Role.USER, Instant.now(), null);
        when(userCredentialsPort.findByIdentifier("user2")).thenReturn(Optional.of(inactiveTotpUser));
        when(passwordVerifier.matches("password", "hashed_pw")).thenReturn(true);

        assertThatThrownBy(() -> handler.login(new LoginCommand("user2", "password")))
            .isInstanceOf(AuthenticationException.UserNotActive.class);
        verifyNoInteractions(challengeStore, accessTokenPort, refreshTokenPort, secondFactor);
    }

    @Test
    void login_secondFactor_notCalledOnUnknownUser() {
        when(userCredentialsPort.findByIdentifier("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.login(new LoginCommand("unknown", "password")))
            .isInstanceOf(AuthenticationException.InvalidCredentials.class);

        verifyNoInteractions(secondFactor);
    }

    @Test
    void login_secondFactor_notCalledOnWrongPassword() {
        when(userCredentialsPort.findByIdentifier("user1")).thenReturn(Optional.of(activeUser));
        when(passwordVerifier.matches("wrong", "hashed_pw")).thenReturn(false);

        assertThatThrownBy(() -> handler.login(new LoginCommand("user1", "wrong")))
            .isInstanceOf(AuthenticationException.InvalidCredentials.class);

        verifyNoInteractions(secondFactor);
    }

    @Test
    void login_success_accessTokenThrows_propagatesException() {
        when(userCredentialsPort.findByIdentifier("user1")).thenReturn(Optional.of(activeUser));
        when(passwordVerifier.matches("password", "hashed_pw")).thenReturn(true);
        when(accessTokenPort.generate(activeUser)).thenThrow(new RuntimeException("jwt store down"));

        assertThatThrownBy(() -> handler.login(new LoginCommand("user1", "password")))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("jwt store down");
        verifyNoInteractions(refreshTokenPort);
    }

    @Test
    void login_success_refreshTokenThrows_propagatesException() {
        when(userCredentialsPort.findByIdentifier("user1")).thenReturn(Optional.of(activeUser));
        when(passwordVerifier.matches("password", "hashed_pw")).thenReturn(true);
        when(accessTokenPort.generate(activeUser)).thenReturn("jwt_access");
        when(refreshTokenPort.generate(eq(activeUser), anyInt()))
            .thenThrow(new RuntimeException("refresh store down"));

        assertThatThrownBy(() -> handler.login(new LoginCommand("user1", "password")))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("refresh store down");
    }

    @Test
    void the_app_alone_gets_a_challenge_and_no_tokens() {
        when(userCredentialsPort.findByIdentifier("user2")).thenReturn(Optional.of(activeUser2));
        when(passwordVerifier.matches("password", "hashed_pw")).thenReturn(true);
        when(secondFactor.usableMethods(activeUser2.id())).thenReturn(EnumSet.of(TwoFactorMethod.APP));
        when(challengeStore.createChallenge(activeUser2.id())).thenReturn("challenge-uuid");

        LoginResult result = handler.login(new LoginCommand("user2", "password"));

        assertThat(result).isEqualTo(new LoginResult.TwoFactorRequired("challenge-uuid", "user2",
            List.of(TwoFactorMethod.APP), null, null));
        verifyNoInteractions(accessTokenPort, refreshTokenPort);
        verify(secondFactor, never()).sendMailCode(any(), any());
    }

    @Test
    void the_mail_alone_sends_its_code_bound_to_the_challenge_and_shows_the_address_masked() {
        when(userCredentialsPort.findByIdentifier("user2")).thenReturn(Optional.of(activeUser2));
        when(passwordVerifier.matches("password", "hashed_pw")).thenReturn(true);
        when(secondFactor.usableMethods(activeUser2.id())).thenReturn(EnumSet.of(TwoFactorMethod.MAIL));
        when(challengeStore.createChallenge(activeUser2.id())).thenReturn("challenge-uuid");
        when(secondFactor.sendMailCode(activeUser2.id(), "challenge-uuid")).thenReturn(new MailCodeDelivery.Sent(60));

        LoginResult result = handler.login(new LoginCommand("user2", "password"));

        assertThat(result).isEqualTo(new LoginResult.TwoFactorRequired("challenge-uuid", "user2",
            List.of(TwoFactorMethod.MAIL), "u••••••2@test.com", new MailCodeDelivery.Sent(60)));
    }

    @Test
    void with_both_methods_nothing_is_sent_until_one_is_chosen() {
        when(userCredentialsPort.findByIdentifier("user2")).thenReturn(Optional.of(activeUser2));
        when(passwordVerifier.matches("password", "hashed_pw")).thenReturn(true);
        when(secondFactor.usableMethods(activeUser2.id())).thenReturn(EnumSet.of(TwoFactorMethod.MAIL, TwoFactorMethod.APP));
        when(challengeStore.createChallenge(activeUser2.id())).thenReturn("challenge-uuid");

        LoginResult.TwoFactorRequired result = (LoginResult.TwoFactorRequired) handler.login(new LoginCommand("user2", "password"));

        assertThat(result.methods()).containsExactly(TwoFactorMethod.APP, TwoFactorMethod.MAIL);
        assertThat(result.maskedEmail()).isEqualTo("u••••••2@test.com");
        assertThat(result.mailCode()).isNull();
        verify(secondFactor, never()).sendMailCode(any(), any());
    }

    @Test
    void a_paused_mail_method_lets_the_password_through_and_is_still_named() {
        when(userCredentialsPort.findByIdentifier("user1")).thenReturn(Optional.of(activeUser));
        when(passwordVerifier.matches("password", "hashed_pw")).thenReturn(true);
        when(secondFactor.usableMethods(activeUser.id())).thenReturn(EnumSet.noneOf(TwoFactorMethod.class));
        when(secondFactor.activeMethods(activeUser.id())).thenReturn(EnumSet.of(TwoFactorMethod.MAIL));
        when(accessTokenPort.generate(activeUser)).thenReturn("jwt_access");
        when(refreshTokenPort.generate(eq(activeUser), anyInt())).thenReturn("raw_refresh");

        LoginResult.Success result = (LoginResult.Success) handler.login(new LoginCommand("user1", "password"));

        assertThat(result.twoFactorMethods()).containsExactly(TwoFactorMethod.MAIL);
        verifyNoInteractions(challengeStore);
    }

    @Test
    void a_locked_out_account_gets_no_challenge_and_no_code_and_is_told_how_long() {
        // Five wrong codes lock the account for a while: a new sign-in would mail a code its check refuses anyway.
        when(userCredentialsPort.findByIdentifier("user2")).thenReturn(Optional.of(activeUser2));
        when(passwordVerifier.matches("password", "hashed_pw")).thenReturn(true);
        when(secondFactor.usableMethods(activeUser2.id())).thenReturn(EnumSet.of(TwoFactorMethod.MAIL));
        when(challengeStore.failedAttempts(activeUser2.id())).thenReturn(5);
        when(challengeStore.lockoutSecondsLeft(activeUser2.id())).thenReturn(600L);

        assertThatThrownBy(() -> handler.login(new LoginCommand("user2", "password")))
            .isInstanceOfSatisfying(AuthenticationException.TwoFactorLockedOut.class,
                locked -> assertThat(locked.retryAfterSeconds()).isEqualTo(600));
        verify(challengeStore, never()).createChallenge(any());
        verify(secondFactor, never()).sendMailCode(any(), any());
    }

    @Test
    void mail_switched_off_between_the_check_and_the_send_lets_the_password_through_like_a_paused_method() {
        when(userCredentialsPort.findByIdentifier("user2")).thenReturn(Optional.of(activeUser2));
        when(passwordVerifier.matches("password", "hashed_pw")).thenReturn(true);
        when(secondFactor.usableMethods(activeUser2.id())).thenReturn(EnumSet.of(TwoFactorMethod.MAIL));
        when(secondFactor.activeMethods(activeUser2.id())).thenReturn(EnumSet.of(TwoFactorMethod.MAIL));
        when(challengeStore.createChallenge(activeUser2.id())).thenReturn("challenge-uuid");
        when(secondFactor.sendMailCode(activeUser2.id(), "challenge-uuid")).thenReturn(new MailCodeDelivery.Unavailable());
        when(accessTokenPort.generate(activeUser2)).thenReturn("jwt_access");
        when(refreshTokenPort.generate(eq(activeUser2), anyInt())).thenReturn("raw_refresh");

        LoginResult result = handler.login(new LoginCommand("user2", "password"));

        assertThat(result).isInstanceOfSatisfying(LoginResult.Success.class,
            success -> assertThat(success.twoFactorMethods()).containsExactly(TwoFactorMethod.MAIL));
        verify(challengeStore).invalidateChallenge("challenge-uuid");
    }
}
