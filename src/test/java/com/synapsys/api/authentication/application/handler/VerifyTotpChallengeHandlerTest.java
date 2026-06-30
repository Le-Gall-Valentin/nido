package com.synapsys.api.authentication.application.handler;

import com.synapsys.api.authentication.application.dto.VerifyTotpChallengeCommand;
import com.synapsys.api.authentication.domain.model.*;
import com.synapsys.api.authentication.domain.port.out.*;
import com.synapsys.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerifyTotpChallengeHandlerTest {

    @Mock TotpChallengeStorePort challengeStore;
    @Mock MfaTotpVerifierPort mfaVerifier;
    @Mock UserCredentialsPort userCredentialsPort;
    @Mock AccessTokenPort accessTokenPort;
    @Mock RefreshTokenIssuerPort refreshTokenPort;
    @Mock RefreshTokenConfigPort tokenConfig;

    private VerifyTotpChallengeHandler handler;

    private final UUID userId = UUID.randomUUID();
    private final UserCredentials creds = new UserCredentials(
        userId, "user1", "user1@test.com", "hash", true, Role.USER
    , Instant.now());

    @BeforeEach
    void setUp() {
        when(tokenConfig.refreshTokenExpiryDays()).thenReturn(30);
        handler = new VerifyTotpChallengeHandler(
            challengeStore, mfaVerifier, userCredentialsPort,
            accessTokenPort, refreshTokenPort, tokenConfig
        );
    }

    @Test
    void verify_validChallenge_validCode_returnsSuccess() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(creds));
        when(mfaVerifier.verifyAndConsume(userId, "123456")).thenReturn(TotpVerificationResult.SUCCESS);
        when(accessTokenPort.generate(creds)).thenReturn("jwt-token");
        when(refreshTokenPort.generate(eq(creds), anyInt())).thenReturn("refresh-token");

        LoginResult.Success result = handler.verify(new VerifyTotpChallengeCommand("challenge-id", "123456"));

        assertThat(result.tokens().accessToken()).isEqualTo("jwt-token");
        assertThat(result.tokens().refreshToken()).isEqualTo("refresh-token");
        assertThat(result.credentials()).isEqualTo(creds);
    }

    @Test
    void verify_validChallenge_validCode_invalidatesChallenge() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(creds));
        when(mfaVerifier.verifyAndConsume(userId, "123456")).thenReturn(TotpVerificationResult.SUCCESS);
        when(accessTokenPort.generate(any())).thenReturn("jwt");
        when(refreshTokenPort.generate(any(), anyInt())).thenReturn("refresh");

        handler.verify(new VerifyTotpChallengeCommand("challenge-id", "123456"));

        verify(challengeStore).invalidateChallenge("challenge-id");
    }

    @Test
    void verify_expiredChallenge_throwsTotpChallengeExpired() {
        when(challengeStore.resolveChallenge("expired-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.verify(new VerifyTotpChallengeCommand("expired-id", "123456")))
            .isInstanceOf(AuthenticationException.TotpChallengeExpired.class);
    }

    @Test
    void verify_invalidCode_throwsTotpCodeInvalid() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(creds));
        when(mfaVerifier.verifyAndConsume(userId, "000000")).thenReturn(TotpVerificationResult.INVALID);
        when(challengeStore.incrementFailedAttempts("challenge-id")).thenReturn(1);

        assertThatThrownBy(() -> handler.verify(new VerifyTotpChallengeCommand("challenge-id", "000000")))
            .isInstanceOf(AuthenticationException.TotpCodeInvalid.class);
    }

    @Test
    void verify_inactiveUser_throwsUserNotActive() {
        UserCredentials inactive = new UserCredentials(userId, "user1", "user1@test.com", "hash",
            false, Role.USER, Instant.now());
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> handler.verify(new VerifyTotpChallengeCommand("challenge-id", "123456")))
            .isInstanceOf(AuthenticationException.UserNotActive.class);
    }

    @Test
    void verify_userNotFound_throwsUserNotFound() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.verify(new VerifyTotpChallengeCommand("challenge-id", "123456")))
            .isInstanceOf(AuthenticationException.UserNotFound.class);
    }

    @Test
    void verify_invalidCode_incrementsAttempts() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(creds));
        when(mfaVerifier.verifyAndConsume(userId, "000000")).thenReturn(TotpVerificationResult.INVALID);
        when(challengeStore.incrementFailedAttempts("challenge-id")).thenReturn(1);

        assertThatThrownBy(() -> handler.verify(new VerifyTotpChallengeCommand("challenge-id", "000000")))
            .isInstanceOf(AuthenticationException.TotpCodeInvalid.class);

        verify(challengeStore).incrementFailedAttempts("challenge-id");
    }

    @Test
    void verify_invalidCode_atMaxAttempts_throwsTotpMaxAttemptsExceededAndInvalidates() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(creds));
        when(mfaVerifier.verifyAndConsume(userId, "000000")).thenReturn(TotpVerificationResult.INVALID);
        when(challengeStore.incrementFailedAttempts("challenge-id")).thenReturn(5);

        assertThatThrownBy(() -> handler.verify(new VerifyTotpChallengeCommand("challenge-id", "000000")))
            .isInstanceOf(AuthenticationException.TotpMaxAttemptsExceeded.class);

        verify(challengeStore).invalidateChallenge("challenge-id");
    }

    @Test
    void verify_validCode_tokenGenerationFails_challengeAlreadyInvalidated() {
        // invalidateChallenge() is called before generate() — if generate() throws,
        // the challenge is consumed (accepted trade-off: user must re-login from scratch)
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(creds));
        when(mfaVerifier.verifyAndConsume(userId, "123456")).thenReturn(TotpVerificationResult.SUCCESS);
        when(accessTokenPort.generate(creds)).thenReturn("jwt");
        when(refreshTokenPort.generate(eq(creds), anyInt())).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> handler.verify(new VerifyTotpChallengeCommand("challenge-id", "123456")))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("DB error");
        verify(challengeStore).invalidateChallenge("challenge-id");
    }

    @Test
    void verify_invalidCode_belowMaxAttempts_doesNotInvalidateChallenge() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(creds));
        when(mfaVerifier.verifyAndConsume(userId, "000000")).thenReturn(TotpVerificationResult.INVALID);
        when(challengeStore.incrementFailedAttempts("challenge-id")).thenReturn(4);

        assertThatThrownBy(() -> handler.verify(new VerifyTotpChallengeCommand("challenge-id", "000000")))
            .isInstanceOf(AuthenticationException.TotpCodeInvalid.class);

        verify(challengeStore, never()).invalidateChallenge(any());
    }

    @Test
    void verify_replayedCode_doesNotIncrementFailedAttempts() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(creds));
        when(mfaVerifier.verifyAndConsume(userId, "123456")).thenReturn(TotpVerificationResult.REPLAYED);

        assertThatThrownBy(() -> handler.verify(new VerifyTotpChallengeCommand("challenge-id", "123456")))
            .isInstanceOf(AuthenticationException.TotpCodeInvalid.class);

        verify(challengeStore, never()).incrementFailedAttempts(any());
    }

    @Test
    void verify_replayedCode_doesNotInvalidateChallenge() {
        when(challengeStore.resolveChallenge("challenge-id")).thenReturn(Optional.of(userId));
        when(userCredentialsPort.findById(userId)).thenReturn(Optional.of(creds));
        when(mfaVerifier.verifyAndConsume(userId, "123456")).thenReturn(TotpVerificationResult.REPLAYED);

        assertThatThrownBy(() -> handler.verify(new VerifyTotpChallengeCommand("challenge-id", "123456")))
            .isInstanceOf(AuthenticationException.TotpCodeInvalid.class);

        verify(challengeStore, never()).invalidateChallenge(any());
    }
}