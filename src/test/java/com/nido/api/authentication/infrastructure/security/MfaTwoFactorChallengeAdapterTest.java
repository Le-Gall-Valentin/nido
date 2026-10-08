package com.nido.api.authentication.infrastructure.security;

import com.nido.api.authentication.domain.model.MailCodeDelivery;
import com.nido.api.authentication.domain.model.SecondFactorCheck;
import com.nido.api.mfa.application.port.in.GetTwoFactorMethodsUseCase;
import com.nido.api.mfa.application.port.in.TwoFactorChallengeUseCase;
import com.nido.api.mfa.domain.model.CodeCheck;
import com.nido.api.mfa.domain.model.CodeDelivery;
import com.nido.api.shared.model.TwoFactorMethod;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MfaTwoFactorChallengeAdapterTest {

    private final GetTwoFactorMethodsUseCase methods = mock(GetTwoFactorMethodsUseCase.class);
    private final TwoFactorChallengeUseCase challenge = mock(TwoFactorChallengeUseCase.class);
    private final MfaTwoFactorChallengeAdapter adapter = new MfaTwoFactorChallengeAdapter(methods, challenge);
    private final UUID jane = UUID.randomUUID();

    @Test
    void every_answer_about_the_mail_code_is_carried_over() {
        when(challenge.sendMailCode(jane, "c")).thenReturn(
            new CodeDelivery.Sent(60), new CodeDelivery.TooSoon(30), new CodeDelivery.LimitReached(420), new CodeDelivery.Unavailable());

        assertThat(adapter.sendMailCode(jane, "c")).isEqualTo(new MailCodeDelivery.Sent(60));
        assertThat(adapter.sendMailCode(jane, "c")).isEqualTo(new MailCodeDelivery.TooSoon(30));
        assertThat(adapter.sendMailCode(jane, "c")).isEqualTo(new MailCodeDelivery.LimitReached(420));
        assertThat(adapter.sendMailCode(jane, "c")).isEqualTo(new MailCodeDelivery.Unavailable());
    }

    @Test
    void every_check_is_carried_over() {
        when(challenge.verify(jane, TwoFactorMethod.APP, "c", "123456"))
            .thenReturn(CodeCheck.SUCCESS, CodeCheck.INVALID, CodeCheck.REPLAYED);

        assertThat(adapter.verify(jane, TwoFactorMethod.APP, "c", "123456")).isEqualTo(SecondFactorCheck.SUCCESS);
        assertThat(adapter.verify(jane, TwoFactorMethod.APP, "c", "123456")).isEqualTo(SecondFactorCheck.INVALID);
        assertThat(adapter.verify(jane, TwoFactorMethod.APP, "c", "123456")).isEqualTo(SecondFactorCheck.REPLAYED);
    }

    @Test
    void the_methods_come_from_mfa() {
        when(methods.activeMethods(jane)).thenReturn(EnumSet.of(TwoFactorMethod.MAIL));
        when(challenge.usableMethods(jane)).thenReturn(EnumSet.noneOf(TwoFactorMethod.class));

        assertThat(adapter.activeMethods(jane)).containsExactly(TwoFactorMethod.MAIL);
        assertThat(adapter.usableMethods(jane)).isEmpty();
    }
}
