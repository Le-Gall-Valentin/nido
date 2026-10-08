package com.nido.api.mfa.application.handler;

import com.nido.api.mfa.application.method.TwoFactorMethodHandler;
import com.nido.api.mfa.application.method.TwoFactorMethods;
import com.nido.api.mfa.domain.model.CodeCheck;
import com.nido.api.mfa.domain.model.CodeDelivery;
import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.mfa.domain.port.out.TwoFactorMethodStorePort;
import com.nido.api.shared.model.TwoFactorMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TwoFactorChallengeHandlerTest {

    private final TwoFactorMethodStorePort store = mock(TwoFactorMethodStorePort.class);
    private final TwoFactorMethodHandler app = mock(TwoFactorMethodHandler.class);
    private final TwoFactorMethodHandler mail = mock(TwoFactorMethodHandler.class);
    private final UUID jane = UUID.randomUUID();
    private TwoFactorChallengeHandler handler;

    @BeforeEach
    void setUp() {
        when(app.method()).thenReturn(TwoFactorMethod.APP);
        when(app.usableNow()).thenReturn(true);
        when(mail.method()).thenReturn(TwoFactorMethod.MAIL);
        handler = new TwoFactorChallengeHandler(new TwoFactorMethods(List.of(app, mail)), store);
    }

    @Test
    void a_paused_method_is_not_asked_for() {
        when(store.activeMethods(jane)).thenReturn(EnumSet.of(TwoFactorMethod.APP, TwoFactorMethod.MAIL));
        when(mail.usableNow()).thenReturn(false);

        assertThat(handler.usableMethods(jane)).containsExactly(TwoFactorMethod.APP);
    }

    @Test
    void the_sign_in_code_is_bound_to_the_challenge() {
        when(mail.sendCode(jane, CodePurpose.LOGIN, "challenge-1")).thenReturn(new CodeDelivery.TooSoon(30));
        when(mail.check(jane, CodePurpose.LOGIN, "challenge-1", "004213")).thenReturn(CodeCheck.SUCCESS);

        assertThat(handler.sendMailCode(jane, "challenge-1")).isEqualTo(new CodeDelivery.TooSoon(30));
        assertThat(handler.verify(jane, TwoFactorMethod.MAIL, "challenge-1", "004213")).isEqualTo(CodeCheck.SUCCESS);
    }
}
