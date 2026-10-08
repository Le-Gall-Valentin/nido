package com.nido.api.mfa.application.method;

import com.nido.api.shared.model.TwoFactorMethod;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TwoFactorMethodsTest {

    private static TwoFactorMethodHandler handler(TwoFactorMethod method) {
        TwoFactorMethodHandler handler = mock(TwoFactorMethodHandler.class);
        when(handler.method()).thenReturn(method);
        return handler;
    }

    @Test
    void each_method_is_found_by_its_name() {
        TwoFactorMethodHandler app = handler(TwoFactorMethod.APP);
        TwoFactorMethodHandler mail = handler(TwoFactorMethod.MAIL);

        TwoFactorMethods methods = new TwoFactorMethods(List.of(mail, app));

        assertThat(methods.of(TwoFactorMethod.APP)).isSameAs(app);
        assertThat(methods.of(TwoFactorMethod.MAIL)).isSameAs(mail);
    }

    @Test
    void only_a_method_whose_codes_are_sent_can_be_asked_for_one() {
        TwoFactorMethodHandler app = handler(TwoFactorMethod.APP);
        CodeSendingMethod mail = mock(CodeSendingMethod.class);
        when(mail.method()).thenReturn(TwoFactorMethod.MAIL);

        TwoFactorMethods methods = new TwoFactorMethods(List.of(app, mail));

        assertThat(methods.sender(TwoFactorMethod.MAIL)).containsSame(mail);
        assertThat(methods.sender(TwoFactorMethod.APP)).isEmpty();
    }

    @Test
    void a_method_nobody_implements_stops_the_start() {
        assertThatThrownBy(() -> new TwoFactorMethods(List.of(handler(TwoFactorMethod.APP))))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("MAIL");
    }

    @Test
    void two_implementations_of_one_method_stop_the_start() {
        assertThatThrownBy(() -> new TwoFactorMethods(List.of(
                handler(TwoFactorMethod.APP), handler(TwoFactorMethod.APP), handler(TwoFactorMethod.MAIL))))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("APP");
    }
}
