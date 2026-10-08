package com.nido.api.mfa.application.handler;

import com.nido.api.mfa.application.method.MailCodeIssuer;
import com.nido.api.mfa.domain.model.CodeCheck;
import com.nido.api.mfa.domain.model.CodeDelivery;
import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.mfa.domain.port.out.TwoFactorMailPort;
import com.nido.api.mfa.domain.port.out.TwoFactorMethodStorePort;
import com.nido.api.shared.model.TwoFactorMethod;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddressChangeCodeHandlerTest {

    private final TwoFactorMethodStorePort store = mock(TwoFactorMethodStorePort.class);
    private final MailCodeIssuer issuer = mock(MailCodeIssuer.class);
    private final TwoFactorMailPort mails = mock(TwoFactorMailPort.class);
    private final AddressChangeCodeHandler handler = new AddressChangeCodeHandler(store, issuer, mails);
    private final UUID jane = UUID.randomUUID();

    @Test
    void the_mail_method_stands_on_the_address_whether_mail_works_now_or_not() {
        when(store.activeMethods(jane)).thenReturn(EnumSet.of(TwoFactorMethod.MAIL), EnumSet.of(TwoFactorMethod.APP));

        assertThat(handler.mailMethodOn(jane)).isTrue();
        assertThat(handler.mailMethodOn(jane)).isFalse();
    }

    @Test
    void the_code_goes_to_the_new_address_and_is_bound_to_it() {
        when(issuer.issue(jane, CodePurpose.EMAIL_CHANGE, "jane@new.fr", "jane@new.fr")).thenReturn(new CodeDelivery.Sent(60));

        assertThat(handler.send(jane, "jane@new.fr")).isEqualTo(new CodeDelivery.Sent(60));
    }

    @Test
    void a_refused_send_is_an_answer_for_identity_to_word_not_an_error_of_mfa() {
        when(issuer.issue(jane, CodePurpose.EMAIL_CHANGE, "jane@new.fr", "jane@new.fr")).thenReturn(new CodeDelivery.TooSoon(30));

        assertThat(handler.send(jane, "jane@new.fr")).isEqualTo(new CodeDelivery.TooSoon(30));
    }

    @Test
    void a_code_counts_only_for_the_address_it_was_sent_to() {
        when(issuer.check(jane, CodePurpose.EMAIL_CHANGE, "jane@new.fr", "004213")).thenReturn(CodeCheck.SUCCESS);
        when(issuer.check(jane, CodePurpose.EMAIL_CHANGE, "jane@other.fr", "004213")).thenReturn(CodeCheck.EXPIRED);

        assertThat(handler.check(jane, "jane@new.fr", "004213")).isEqualTo(CodeCheck.SUCCESS);
        assertThat(handler.check(jane, "jane@other.fr", "004213")).isEqualTo(CodeCheck.EXPIRED);
    }

    @Test
    void an_address_that_could_not_be_proven_takes_the_mail_method_with_it_and_its_holder_is_told() {
        // Left on, the method would send every future code to an address nobody proved: a typo locks the holder
        // out once mail is back, and whoever made the change gets the codes.
        when(store.activeMethods(jane)).thenReturn(EnumSet.of(TwoFactorMethod.APP, TwoFactorMethod.MAIL));
        when(store.disable(jane, TwoFactorMethod.MAIL)).thenReturn(true);

        handler.forgoMailMethod(jane);

        verify(store).disable(jane, TwoFactorMethod.MAIL);
        verify(issuer).forget(jane);
        verify(mails).methodDisabled(jane, TwoFactorMethod.MAIL, true);
    }

    @Test
    void a_mail_method_already_gone_tells_nobody() {
        when(store.activeMethods(jane)).thenReturn(EnumSet.noneOf(TwoFactorMethod.class));
        when(store.disable(jane, TwoFactorMethod.MAIL)).thenReturn(false);

        handler.forgoMailMethod(jane);

        verify(mails, never()).methodDisabled(any(), any(), org.mockito.ArgumentMatchers.anyBoolean());
    }
}
