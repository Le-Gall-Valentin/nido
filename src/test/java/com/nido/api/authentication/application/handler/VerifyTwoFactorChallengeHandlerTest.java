package com.nido.api.authentication.application.handler;

import com.nido.api.authentication.application.dto.VerifyTwoFactorChallengeCommand;
import com.nido.api.authentication.domain.model.AuthenticationException;
import com.nido.api.authentication.domain.model.LoginResult;
import com.nido.api.authentication.domain.model.SecondFactorCheck;
import com.nido.api.authentication.domain.model.UserCredentials;
import com.nido.api.authentication.domain.model.*;
import com.nido.api.authentication.domain.port.out.*;
import com.nido.api.authentication.domain.port.out.*;
import com.nido.api.shared.model.Role;
import com.nido.api.shared.model.TwoFactorMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.Optional;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerifyTwoFactorChallengeHandlerTest {

    @Mock
    TwoFactorChallengeStorePort challengeStore;
    @Mock
    TwoFactorChallengePort secondFactor;
    @Mock UserCredentialsPort userCredentialsPort;
    @Mock
    AccessTokenPort accessTokenPort;
    @Mock
    RefreshTokenIssuerPort refreshTokenPort;
    @Mock
    RefreshTokenConfigPort tokenConfig;

    private VerifyTwoFactorChallengeHandler handler;

    private final UUID userId = UUID.randomUUID();
    private final UserCredentials creds = new UserCredentials(
        userId, "user1", "user1@test.com", "hash", true, Role.USER
    , Instant.now(), null);

    @BeforeEach
    void setUp() {
        lenient().when(tokenConfig.refreshTokenExpiryDays()).thenReturn(30);
        lenient().when(secondFactor.activeMethods(any())).thenReturn(EnumSet.of(TwoFactorMethod.APP));
        lenient().when(secondFactor.usableMethods(any())).thenReturn(EnumSet.of(TwoFactorMethod.APP));
        handler = new VerifyTwoFactorChallengeHandler(
            challengeStore, secondFactor, userCredentialsPort,
            accessTokenPort, refreshTokenPort, tokenConfig
        );
    }

    @Test
    void verify_validChallenge_validCode_returnsSuccess() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(creds));
        when(secondFactor.verify(userId, TwoFactorMethod.APP, "challenge-id", "123456")).thenReturn(SecondFactorCheck.SUCCESS);
        when(accessTokenPort.generate(creds)).thenReturn("jwt-token");
        when(refreshTokenPort.generate(eq(creds), anyInt())).thenReturn("refresh-token");

        LoginResult.Success result = handler.verify(new VerifyTwoFactorChallengeCommand("challenge-id", TwoFactorMethod.APP, "123456"));

        assertThat(result.tokens().accessToken()).isEqualTo("jwt-token");
        assertThat(result.tokens().refreshToken()).isEqualTo("refresh-token");
        assertThat(result.credentials()).isEqualTo(creds);
    }

    @Test
    void verify_validChallenge_validCode_invalidatesChallenge() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(creds));
        when(secondFactor.verify(userId, TwoFactorMethod.APP, "challenge-id", "123456")).thenReturn(SecondFactorCheck.SUCCESS);
        when(accessTokenPort.generate(any())).thenReturn("jwt");
        when(refreshTokenPort.generate(any(), anyInt())).thenReturn("refresh");

        handler.verify(new VerifyTwoFactorChallengeCommand("challenge-id", TwoFactorMethod.APP, "123456"));

        verify(challengeStore).invalidateChallenge("challenge-id");
    }

    @Test
    void verify_expiredChallenge_throwsTotpChallengeExpired() {
        when(challengeStore.resolveChallenge("expired-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.verify(new VerifyTwoFactorChallengeCommand("expired-id", TwoFactorMethod.APP, "123456")))
            .isInstanceOf(AuthenticationException.TwoFactorChallengeExpired.class);
    }

    @Test
    void verify_invalidCode_throwsTotpCodeInvalid() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(creds));
        when(secondFactor.verify(userId, TwoFactorMethod.APP, "challenge-id", "000000")).thenReturn(SecondFactorCheck.INVALID);
        when(challengeStore.recordFailedAttempt(userId)).thenReturn(1);

        assertThatThrownBy(() -> handler.verify(new VerifyTwoFactorChallengeCommand("challenge-id", TwoFactorMethod.APP, "000000")))
            .isInstanceOf(AuthenticationException.TwoFactorCodeInvalid.class);
    }

    @Test
    void verify_inactiveUser_throwsUserNotActive() {
        UserCredentials inactive = new UserCredentials(userId, "user1", "user1@test.com", "hash",
            false, Role.USER, Instant.now(), null);
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> handler.verify(new VerifyTwoFactorChallengeCommand("challenge-id", TwoFactorMethod.APP, "123456")))
            .isInstanceOf(AuthenticationException.UserNotActive.class);
    }

    @Test
    void verify_userNotFound_throwsUserNotFound() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.verify(new VerifyTwoFactorChallengeCommand("challenge-id", TwoFactorMethod.APP, "123456")))
            .isInstanceOf(AuthenticationException.UserNotFound.class);
    }

    @Test
    void verify_invalidCode_recordsTheAttemptAgainstTheAccount() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(creds));
        when(secondFactor.verify(userId, TwoFactorMethod.APP, "challenge-id", "000000")).thenReturn(SecondFactorCheck.INVALID);
        when(challengeStore.recordFailedAttempt(userId)).thenReturn(1);

        assertThatThrownBy(() -> handler.verify(new VerifyTwoFactorChallengeCommand("challenge-id", TwoFactorMethod.APP, "000000")))
            .isInstanceOf(AuthenticationException.TwoFactorCodeInvalid.class);

        verify(challengeStore).recordFailedAttempt(userId);
    }

    @Test
    void verify_invalidCode_atMaxAttempts_throwsTotpMaxAttemptsExceededAndInvalidates() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(creds));
        when(secondFactor.verify(userId, TwoFactorMethod.APP, "challenge-id", "000000")).thenReturn(SecondFactorCheck.INVALID);
        when(challengeStore.recordFailedAttempt(userId)).thenReturn(5);

        assertThatThrownBy(() -> handler.verify(new VerifyTwoFactorChallengeCommand("challenge-id", TwoFactorMethod.APP, "000000")))
            .isInstanceOf(AuthenticationException.TwoFactorMaxAttemptsExceeded.class);

        verify(challengeStore).invalidateChallenge("challenge-id");
    }

    @Test
    void verify_validCode_tokenGenerationFails_challengeAlreadyInvalidated() {
        // invalidateChallenge() is called before generate() — if generate() throws,
        // the challenge is consumed (accepted trade-off: user must re-login from scratch)
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(creds));
        when(secondFactor.verify(userId, TwoFactorMethod.APP, "challenge-id", "123456")).thenReturn(SecondFactorCheck.SUCCESS);
        when(accessTokenPort.generate(creds)).thenReturn("jwt");
        when(refreshTokenPort.generate(eq(creds), anyInt())).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> handler.verify(new VerifyTwoFactorChallengeCommand("challenge-id", TwoFactorMethod.APP, "123456")))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("DB error");
        verify(challengeStore).invalidateChallenge("challenge-id");
    }

    @Test
    void verify_invalidCode_belowMaxAttempts_doesNotInvalidateChallenge() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(creds));
        when(secondFactor.verify(userId, TwoFactorMethod.APP, "challenge-id", "000000")).thenReturn(SecondFactorCheck.INVALID);
        when(challengeStore.recordFailedAttempt(userId)).thenReturn(4);

        assertThatThrownBy(() -> handler.verify(new VerifyTwoFactorChallengeCommand("challenge-id", TwoFactorMethod.APP, "000000")))
            .isInstanceOf(AuthenticationException.TwoFactorCodeInvalid.class);

        verify(challengeStore, never()).invalidateChallenge(any());
    }

    @Test
    void verify_replayedCode_doesNotIncrementFailedAttempts() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(creds));
        when(secondFactor.verify(userId, TwoFactorMethod.APP, "challenge-id", "123456")).thenReturn(SecondFactorCheck.REPLAYED);

        assertThatThrownBy(() -> handler.verify(new VerifyTwoFactorChallengeCommand("challenge-id", TwoFactorMethod.APP, "123456")))
            .isInstanceOf(AuthenticationException.TwoFactorCodeInvalid.class);

        verify(challengeStore, never()).recordFailedAttempt(any());
    }

    @Test
    void verify_replayedCode_doesNotInvalidateChallenge() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(creds));
        when(secondFactor.verify(userId, TwoFactorMethod.APP, "challenge-id", "123456")).thenReturn(SecondFactorCheck.REPLAYED);

        assertThatThrownBy(() -> handler.verify(new VerifyTwoFactorChallengeCommand("challenge-id", TwoFactorMethod.APP, "123456")))
            .isInstanceOf(AuthenticationException.TwoFactorCodeInvalid.class);

        verify(challengeStore, never()).invalidateChallenge(any());
    }
    // ─── B4 : le compteur suit le compte, pas le challenge ────────────────

    @Test
    void verify_whenTheAccountIsAlreadyLockedOut_refusesWithoutEvenCheckingTheCode() {
        // The bypass this closes: a caller who has used up their attempts obtained a clean
        // counter simply by logging in again, because the counter hung off the challenge id
        // and every login mints a new one. Keyed on the account, a fresh challenge changes
        // nothing — and the code is not even looked at.
        when(challengeStore.resolveChallenge("fresh-challenge")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(creds));
        when(challengeStore.failedAttempts(userId)).thenReturn(5);

        assertThatThrownBy(() -> handler.verify(new VerifyTwoFactorChallengeCommand("fresh-challenge", TwoFactorMethod.APP, "123456")))
            .isInstanceOf(AuthenticationException.TwoFactorMaxAttemptsExceeded.class);

        verify(secondFactor, never()).verify(any(), any(), any(), any());
    }

    @Test
    void verify_whenAlreadyLockedOut_doesNotRenewTheWindow() {
        // Otherwise hammering a locked account would keep pushing the TTL out, and a
        // brute-force guard would double as a way to deny the owner service indefinitely.
        when(challengeStore.resolveChallenge("fresh-challenge")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(creds));
        when(challengeStore.failedAttempts(userId)).thenReturn(7);

        assertThatThrownBy(() -> handler.verify(new VerifyTwoFactorChallengeCommand("fresh-challenge", TwoFactorMethod.APP, "000000")))
            .isInstanceOf(AuthenticationException.TwoFactorMaxAttemptsExceeded.class);

        verify(challengeStore, never()).recordFailedAttempt(any());
    }

    @Test
    void verify_belowTheLimit_stillLetsTheCodeThrough() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(creds));
        when(challengeStore.failedAttempts(userId)).thenReturn(4);
        when(secondFactor.verify(userId, TwoFactorMethod.APP, "challenge-id", "123456")).thenReturn(SecondFactorCheck.SUCCESS);
        when(accessTokenPort.generate(creds)).thenReturn("jwt");
        when(refreshTokenPort.generate(eq(creds), anyInt())).thenReturn("refresh");

        LoginResult.Success result = handler.verify(new VerifyTwoFactorChallengeCommand("challenge-id", TwoFactorMethod.APP, "123456"));

        assertThat(result.credentials()).isEqualTo(creds);
    }

    @Test
    void verify_validCode_clearsTheAccountCounter() {
        // Someone who mistyped three codes before getting it right must not carry those
        // failures into their next login.
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(creds));
        when(secondFactor.verify(userId, TwoFactorMethod.APP, "challenge-id", "123456")).thenReturn(SecondFactorCheck.SUCCESS);
        when(accessTokenPort.generate(creds)).thenReturn("jwt");
        when(refreshTokenPort.generate(eq(creds), anyInt())).thenReturn("refresh");

        handler.verify(new VerifyTwoFactorChallengeCommand("challenge-id", TwoFactorMethod.APP, "123456"));

        verify(challengeStore).clearFailedAttempts(userId);
    }

    @Test
    void verify_invalidCode_doesNotClearTheAccountCounter() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(creds));
        when(secondFactor.verify(userId, TwoFactorMethod.APP, "challenge-id", "000000")).thenReturn(SecondFactorCheck.INVALID);
        when(challengeStore.recordFailedAttempt(userId)).thenReturn(2);

        assertThatThrownBy(() -> handler.verify(new VerifyTwoFactorChallengeCommand("challenge-id", TwoFactorMethod.APP, "000000")))
            .isInstanceOf(AuthenticationException.TwoFactorCodeInvalid.class);

        verify(challengeStore, never()).clearFailedAttempts(any());
    }

    @Test
    void a_method_removed_during_the_challenge_is_refused_by_name() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(creds));
        when(secondFactor.activeMethods(userId)).thenReturn(EnumSet.of(TwoFactorMethod.APP));

        assertThatThrownBy(() -> handler.verify(new VerifyTwoFactorChallengeCommand("challenge-id", TwoFactorMethod.MAIL, "004213")))
            .isInstanceOf(AuthenticationException.MethodNotEnabled.class);
        verify(challengeStore, never()).recordFailedAttempt(any());
    }

    @Test
    void a_paused_method_is_refused_by_name() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(creds));
        when(secondFactor.activeMethods(userId)).thenReturn(EnumSet.of(TwoFactorMethod.MAIL));
        when(secondFactor.usableMethods(userId)).thenReturn(EnumSet.noneOf(TwoFactorMethod.class));

        assertThatThrownBy(() -> handler.verify(new VerifyTwoFactorChallengeCommand("challenge-id", TwoFactorMethod.MAIL, "004213")))
            .isInstanceOf(AuthenticationException.MethodUnavailable.class);
    }
}
