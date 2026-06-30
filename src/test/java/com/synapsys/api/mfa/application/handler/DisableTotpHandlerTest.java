package com.synapsys.api.mfa.application.handler;

import com.synapsys.api.mfa.application.dto.DisableTotpCommand;
import com.synapsys.api.mfa.domain.model.MfaException;
import com.synapsys.api.mfa.domain.model.UserTotpProfile;
import com.synapsys.api.mfa.domain.port.out.TotpCodeReplayPort;
import com.synapsys.api.mfa.domain.port.out.TotpCodeValidatorPort;
import com.synapsys.api.mfa.domain.port.out.UserTotpLifecyclePort;
import com.synapsys.api.mfa.domain.port.out.UserTotpQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisableTotpHandlerTest {

    @Mock UserTotpQueryPort userTotpQuery;
    @Mock UserTotpLifecyclePort userTotpLifecyclePort;
    @Mock TotpCodeValidatorPort codeValidator;
    @Mock TotpCodeReplayPort codeReplay;

    private DisableTotpHandler handler;

    private final UUID userId = UUID.randomUUID();
    private final String secret = "MYSECRET";

    @BeforeEach
    void setUp() {
        handler = new DisableTotpHandler(userTotpQuery, userTotpLifecyclePort, codeValidator, codeReplay);
    }

    @Test
    void disable_validCode_disablesTotp() {
        UserTotpProfile profile = new UserTotpProfile(userId,true, Optional.of(secret));
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));
        when(codeValidator.isValid(secret, "123456")).thenReturn(true);
        when(codeReplay.markCodeUsedIfAbsent(userId, "123456")).thenReturn(true);

        assertThatCode(() -> handler.disable(new DisableTotpCommand(userId, "123456")))
            .doesNotThrowAnyException();

        verify(userTotpLifecyclePort).disableTotp(userId);
    }

    @Test
    void disable_userNotFound_throwsUserNotFound() {
        when(userTotpQuery.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.disable(new DisableTotpCommand(userId, "123456")))
            .isInstanceOf(MfaException.UserNotFound.class);
        verifyNoInteractions(codeValidator, codeReplay, userTotpLifecyclePort);
    }

    @Test
    void disable_totpEnabledButNoSecret_throwsTotpSetupNotStarted() {
        UserTotpProfile profile = new UserTotpProfile(userId, true, Optional.empty());
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> handler.disable(new DisableTotpCommand(userId, "123456")))
            .isInstanceOf(MfaException.TotpSetupNotStarted.class);
        verifyNoInteractions(codeValidator, codeReplay);
    }

    @Test
    void disable_totpNotEnabled_throwsTotpNotEnabled() {
        UserTotpProfile profile = new UserTotpProfile(userId,false, Optional.empty());
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> handler.disable(new DisableTotpCommand(userId, "123456")))
            .isInstanceOf(MfaException.TotpNotEnabled.class);
    }

    @Test
    void disable_wrongCode_throwsTotpCodeInvalid() {
        UserTotpProfile profile = new UserTotpProfile(userId,true, Optional.of(secret));
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));
        when(codeValidator.isValid(secret, "000000")).thenReturn(false);

        assertThatThrownBy(() -> handler.disable(new DisableTotpCommand(userId, "000000")))
            .isInstanceOf(MfaException.TotpCodeInvalid.class);
    }

    @Test
    void disable_replayedCode_throwsTotpCodeInvalid() {
        UserTotpProfile profile = new UserTotpProfile(userId,true, Optional.of(secret));
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));
        when(codeValidator.isValid(secret, "123456")).thenReturn(true);
        when(codeReplay.markCodeUsedIfAbsent(userId, "123456")).thenReturn(false);

        assertThatThrownBy(() -> handler.disable(new DisableTotpCommand(userId, "123456")))
            .isInstanceOf(MfaException.TotpCodeInvalid.class);
    }
}