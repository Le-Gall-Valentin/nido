package com.nido.api.mfa.application.handler;

import com.nido.api.mfa.application.dto.ConfirmTotpCommand;
import com.nido.api.mfa.domain.model.MfaException;
import com.nido.api.mfa.domain.model.UserTotpProfile;
import com.nido.api.mfa.domain.port.out.TotpCodeReplayPort;
import com.nido.api.mfa.domain.port.out.TotpCodeValidatorPort;
import com.nido.api.mfa.domain.port.out.TotpConfirmAttemptPort;
import com.nido.api.mfa.domain.port.out.UserTotpLifecyclePort;
import com.nido.api.mfa.domain.port.out.UserTotpQueryPort;
import com.nido.api.mfa.domain.port.out.PendingTotpEnrolmentPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmTotpHandlerTest {

    @Mock UserTotpQueryPort userTotpQuery;
    @Mock TotpCodeValidatorPort codeValidator;
    @Mock UserTotpLifecyclePort userTotpLifecyclePort;
    @Mock TotpCodeReplayPort codeReplay;
    @Mock TotpConfirmAttemptPort confirmAttemptPort;
    @Mock PendingTotpEnrolmentPort pendingEnrolment;

    private ConfirmTotpHandler handler;

    private final UUID userId = UUID.randomUUID();
    private final String secret = "MYSECRET";

    @BeforeEach
    void setUp() {
        handler = new ConfirmTotpHandler(userTotpQuery, codeValidator, userTotpLifecyclePort,
                                         codeReplay, confirmAttemptPort, pendingEnrolment);
    }

    @Test
    void confirm_validCode_enablesTotp() {
        UserTotpProfile profile = new UserTotpProfile(userId,false, Optional.of(secret));
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));
        when(pendingEnrolment.find(userId)).thenReturn(Optional.of(secret));
        when(codeValidator.isValid(secret, "123456")).thenReturn(true);
        when(codeReplay.markCodeUsedIfAbsent(userId, "123456")).thenReturn(true);

        assertThatCode(() -> handler.confirm(new ConfirmTotpCommand(userId, "123456")))
            .doesNotThrowAnyException();

        verify(userTotpLifecyclePort).enableTotp(userId, secret);
    }

    @Test
    void confirm_noEnrolmentUnderWay_throwsTotpSetupNotStarted() {
        // Never started, or started and expired — one answer for both, on purpose.
        UserTotpProfile profile = new UserTotpProfile(userId,false, Optional.empty());
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));
        when(pendingEnrolment.find(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.confirm(new ConfirmTotpCommand(userId, "123456")))
            .isInstanceOf(MfaException.TotpSetupNotStarted.class);
    }

    @Test
    void confirm_wrongCode_throwsTotpCodeInvalid() {
        UserTotpProfile profile = new UserTotpProfile(userId,false, Optional.of(secret));
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));
        when(pendingEnrolment.find(userId)).thenReturn(Optional.of(secret));
        when(codeValidator.isValid(secret, "000000")).thenReturn(false);

        assertThatThrownBy(() -> handler.confirm(new ConfirmTotpCommand(userId, "000000")))
            .isInstanceOf(MfaException.TotpCodeInvalid.class);
    }

    @Test
    void confirm_userNotFound_throwsUserNotFound() {
        when(userTotpQuery.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.confirm(new ConfirmTotpCommand(userId, "123456")))
            .isInstanceOf(MfaException.UserNotFound.class);
        verifyNoInteractions(codeValidator, codeReplay, userTotpLifecyclePort);
    }

    @Test
    void confirm_totpAlreadyEnabled_throwsTotpAlreadyEnabled() {
        UserTotpProfile profile = new UserTotpProfile(userId, true, Optional.of(secret));
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> handler.confirm(new ConfirmTotpCommand(userId, "123456")))
            .isInstanceOf(MfaException.TotpAlreadyEnabled.class);
        // Not even looked up: an account that already has TOTP is refused before anything else.
        verifyNoInteractions(codeValidator, codeReplay, userTotpLifecyclePort, pendingEnrolment);
    }

    @Test
    void confirm_wrongCode_incrementsAttemptCounter() {
        UserTotpProfile profile = new UserTotpProfile(userId, false, Optional.of(secret));
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));
        when(pendingEnrolment.find(userId)).thenReturn(Optional.of(secret));
        when(codeValidator.isValid(secret, "000000")).thenReturn(false);
        when(confirmAttemptPort.incrementAndGetAttempts(userId)).thenReturn(1);

        assertThatThrownBy(() -> handler.confirm(new ConfirmTotpCommand(userId, "000000")))
            .isInstanceOf(MfaException.TotpCodeInvalid.class);

        verify(confirmAttemptPort).incrementAndGetAttempts(userId);
        // The enrolment is read to get the secret to check against, but a single wrong code must
        // not throw it away — only running out of attempts does that.
        verify(pendingEnrolment, never()).discard(userId);
    }

    @Test
    void confirm_maxAttemptsReached_clearsPendingSecretAndThrows() {
        UserTotpProfile profile = new UserTotpProfile(userId, false, Optional.of(secret));
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));
        when(pendingEnrolment.find(userId)).thenReturn(Optional.of(secret));
        when(codeValidator.isValid(secret, "000000")).thenReturn(false);
        when(confirmAttemptPort.incrementAndGetAttempts(userId)).thenReturn(5);

        assertThatThrownBy(() -> handler.confirm(new ConfirmTotpCommand(userId, "000000")))
            .isInstanceOf(MfaException.TotpConfirmMaxAttemptsExceeded.class);

        verify(pendingEnrolment).discard(userId);
        verify(confirmAttemptPort).clearAttempts(userId);
        verifyNoInteractions(userTotpLifecyclePort);
    }

    @Test
    void confirm_success_clearsAttemptCounter() {
        UserTotpProfile profile = new UserTotpProfile(userId, false, Optional.of(secret));
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));
        when(pendingEnrolment.find(userId)).thenReturn(Optional.of(secret));
        when(codeValidator.isValid(secret, "123456")).thenReturn(true);
        when(codeReplay.markCodeUsedIfAbsent(userId, "123456")).thenReturn(true);

        assertThatCode(() -> handler.confirm(new ConfirmTotpCommand(userId, "123456")))
            .doesNotThrowAnyException();

        verify(confirmAttemptPort).clearAttempts(userId);
        verify(userTotpLifecyclePort).enableTotp(userId, secret);
    }

    @Test
    void confirm_replayedCode_doesNotIncrementAttempts() {
        UserTotpProfile profile = new UserTotpProfile(userId, false, Optional.of(secret));
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));
        when(pendingEnrolment.find(userId)).thenReturn(Optional.of(secret));
        when(codeValidator.isValid(secret, "123456")).thenReturn(true);
        when(codeReplay.markCodeUsedIfAbsent(userId, "123456")).thenReturn(false);

        assertThatThrownBy(() -> handler.confirm(new ConfirmTotpCommand(userId, "123456")))
            .isInstanceOf(MfaException.TotpCodeInvalid.class);

        verifyNoInteractions(confirmAttemptPort, userTotpLifecyclePort);
    }

    @Test
    void confirm_replayedCode_throwsTotpCodeInvalid() {
        UserTotpProfile profile = new UserTotpProfile(userId,false, Optional.of(secret));
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));
        when(pendingEnrolment.find(userId)).thenReturn(Optional.of(secret));
        when(codeValidator.isValid(secret, "123456")).thenReturn(true);
        when(codeReplay.markCodeUsedIfAbsent(userId, "123456")).thenReturn(false);

        assertThatThrownBy(() -> handler.confirm(new ConfirmTotpCommand(userId, "123456")))
            .isInstanceOf(MfaException.TotpCodeInvalid.class);
    }
}