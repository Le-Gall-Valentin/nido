package com.nido.api.mfa.application.handler;

import com.nido.api.mfa.application.method.MailCodeIssuer;
import com.nido.api.mfa.domain.model.CodeCheck;
import com.nido.api.mfa.domain.model.CodeDelivery;
import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.mfa.domain.model.MfaException;
import com.nido.api.mfa.domain.port.out.MailAvailabilityPort;
import com.nido.api.mfa.domain.port.out.TwoFactorMethodStorePort;
import com.nido.api.shared.model.TwoFactorMethod;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AddressChangeCodeHandlerTest {

    private final TwoFactorMethodStorePort store = mock(TwoFactorMethodStorePort.class);
    private final MailAvailabilityPort availability = mock(MailAvailabilityPort.class);
    private final MailCodeIssuer issuer = mock(MailCodeIssuer.class);
    private final AddressChangeCodeHandler handler = new AddressChangeCodeHandler(store, availability, issuer);
    private final UUID jane = UUID.randomUUID();

    @Test
    void a_code_is_required_only_while_the_mail_method_is_on_and_mail_works() {
        when(store.activeMethods(jane)).thenReturn(EnumSet.of(TwoFactorMethod.MAIL));
        when(availability.isAvailable()).thenReturn(true, false);

        assertThat(handler.required(jane)).isTrue();
        assertThat(handler.required(jane)).as("mail off: the method is paused").isFalse();

        when(store.activeMethods(jane)).thenReturn(EnumSet.of(TwoFactorMethod.APP));
        when(availability.isAvailable()).thenReturn(true);
        assertThat(handler.required(jane)).isFalse();
    }

    @Test
    void the_code_goes_to_the_new_address_and_is_bound_to_it() {
        when(issuer.issue(jane, CodePurpose.EMAIL_CHANGE, "jane@new.fr", "jane@new.fr")).thenReturn(new CodeDelivery.Sent(60));

        assertThat(handler.send(jane, "jane@new.fr")).isEqualTo(60);
    }

    @Test
    void a_refused_send_is_the_error_a_screen_can_show() {
        when(issuer.issue(jane, CodePurpose.EMAIL_CHANGE, "jane@new.fr", "jane@new.fr")).thenReturn(new CodeDelivery.TooSoon(30));

        assertThatThrownBy(() -> handler.send(jane, "jane@new.fr")).isInstanceOf(MfaException.ResendTooSoon.class);
    }

    @Test
    void a_code_counts_only_for_the_address_it_was_sent_to() {
        when(issuer.check(jane, CodePurpose.EMAIL_CHANGE, "jane@new.fr", "004213")).thenReturn(CodeCheck.SUCCESS);
        when(issuer.check(jane, CodePurpose.EMAIL_CHANGE, "jane@other.fr", "004213")).thenReturn(CodeCheck.INVALID);

        assertThat(handler.check(jane, "jane@new.fr", "004213")).isTrue();
        assertThat(handler.check(jane, "jane@other.fr", "004213")).isFalse();
    }
}
