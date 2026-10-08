package com.nido.api.mfa.application.handler;

import com.nido.api.mfa.application.method.TwoFactorMethodHandler;
import com.nido.api.mfa.application.method.TwoFactorMethods;
import com.nido.api.mfa.domain.model.CodeCheck;
import com.nido.api.mfa.domain.model.CodeDelivery;
import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.mfa.domain.model.EnrolmentStarted;
import com.nido.api.mfa.domain.model.MethodState;
import com.nido.api.mfa.domain.model.MfaException;
import com.nido.api.mfa.domain.port.out.TwoFactorMailPort;
import com.nido.api.mfa.domain.port.out.TwoFactorMethodStorePort;
import com.nido.api.shared.model.TwoFactorMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManageTwoFactorMethodsHandlerTest {

    private final TwoFactorMethodStorePort store = mock(TwoFactorMethodStorePort.class);
    private final TwoFactorMailPort mails = mock(TwoFactorMailPort.class);
    private final TwoFactorMethodHandler app = mock(TwoFactorMethodHandler.class);
    private final TwoFactorMethodHandler mail = mock(TwoFactorMethodHandler.class);
    private final UUID jane = UUID.randomUUID();
    private ManageTwoFactorMethodsHandler handler;

    @BeforeEach
    void setUp() {
        when(app.method()).thenReturn(TwoFactorMethod.APP);
        when(app.usableNow()).thenReturn(true);
        when(mail.method()).thenReturn(TwoFactorMethod.MAIL);
        when(mail.usableNow()).thenReturn(true);
        when(mail.deliversCodes()).thenReturn(true);
        when(store.activeMethods(jane)).thenReturn(EnumSet.noneOf(TwoFactorMethod.class));
        handler = new ManageTwoFactorMethodsHandler(new TwoFactorMethods(List.of(app, mail)), store, mails);
    }

    @Test
    void every_method_is_listed_with_whether_it_is_on_and_usable() {
        when(store.activeMethods(jane)).thenReturn(EnumSet.of(TwoFactorMethod.MAIL));
        when(mail.usableNow()).thenReturn(false);

        assertThat(handler.methods(jane)).containsExactly(
            new MethodState(TwoFactorMethod.APP, false, true),
            new MethodState(TwoFactorMethod.MAIL, true, false));
    }

    @Test
    void a_method_already_on_or_unusable_cannot_be_started() {
        when(store.activeMethods(jane)).thenReturn(EnumSet.of(TwoFactorMethod.APP));
        assertThatThrownBy(() -> handler.startEnrolment(jane, TwoFactorMethod.APP))
            .isInstanceOf(MfaException.MethodAlreadyEnabled.class);

        when(mail.usableNow()).thenReturn(false);
        assertThatThrownBy(() -> handler.startEnrolment(jane, TwoFactorMethod.MAIL))
            .isInstanceOf(MfaException.MethodUnavailable.class);
    }

    @Test
    void starting_hands_back_what_the_method_started_with() {
        when(mail.startEnrolment(jane)).thenReturn(new EnrolmentStarted.MailEnrolment("jane@example.fr", 60));

        assertThat(handler.startEnrolment(jane, TwoFactorMethod.MAIL))
            .isEqualTo(new EnrolmentStarted.MailEnrolment("jane@example.fr", 60));
    }

    @Test
    void a_proven_app_is_written_with_its_secret_then_its_enrolment_forgotten_and_told() {
        when(app.confirmEnrolment(jane, "123456")).thenReturn(Optional.of("SECRET"));

        handler.confirmEnrolment(jane, TwoFactorMethod.APP, "123456");

        var order = inOrder(store, app, mails);
        order.verify(store).enable(jane, TwoFactorMethod.APP, "SECRET");
        order.verify(app).forgetPending(jane);
        order.verify(mails).methodEnabled(jane, TwoFactorMethod.APP);
    }

    @Test
    void a_proven_mail_is_written_without_a_secret() {
        when(mail.confirmEnrolment(jane, "004213")).thenReturn(Optional.empty());

        handler.confirmEnrolment(jane, TwoFactorMethod.MAIL, "004213");

        verify(store).enable(jane, TwoFactorMethod.MAIL, null);
    }

    @Test
    void confirming_mail_once_mail_is_off_says_so() {
        when(mail.usableNow()).thenReturn(false);

        assertThatThrownBy(() -> handler.confirmEnrolment(jane, TwoFactorMethod.MAIL, "004213"))
            .isInstanceOf(MfaException.MethodUnavailable.class);
        verify(store, never()).enable(any(), any(), any());
    }

    @Test
    void only_the_mail_sends_a_code_to_turn_it_off_and_only_when_on() {
        assertThatThrownBy(() -> handler.sendDisableCode(jane, TwoFactorMethod.APP))
            .isInstanceOf(MfaException.MethodSendsNoCode.class);
        assertThatThrownBy(() -> handler.sendDisableCode(jane, TwoFactorMethod.MAIL))
            .isInstanceOf(MfaException.MethodNotEnabled.class);

        when(store.activeMethods(jane)).thenReturn(EnumSet.of(TwoFactorMethod.MAIL));
        when(mail.sendCode(jane, CodePurpose.DISABLE, jane.toString()))
            .thenReturn(new CodeDelivery.Sent(60), new CodeDelivery.TooSoon(30));
        assertThat(handler.sendDisableCode(jane, TwoFactorMethod.MAIL)).isEqualTo(60);
        assertThatThrownBy(() -> handler.sendDisableCode(jane, TwoFactorMethod.MAIL))
            .isInstanceOf(MfaException.ResendTooSoon.class);
    }

    @Test
    void turning_a_method_off_needs_its_proof() {
        when(store.activeMethods(jane)).thenReturn(EnumSet.of(TwoFactorMethod.APP));
        when(app.check(jane, CodePurpose.DISABLE, jane.toString(), "000000")).thenReturn(CodeCheck.INVALID);

        assertThatThrownBy(() -> handler.disable(jane, TwoFactorMethod.APP, "000000"))
            .isInstanceOf(MfaException.CodeInvalid.class);
        assertThatThrownBy(() -> handler.disable(jane, TwoFactorMethod.APP, null))
            .isInstanceOf(MfaException.CodeInvalid.class);
        verify(store, never()).disable(any(), any());
    }

    @Test
    void a_method_turned_off_says_whether_another_still_protects_the_account() {
        when(store.activeMethods(jane)).thenReturn(EnumSet.of(TwoFactorMethod.APP, TwoFactorMethod.MAIL));
        when(app.check(jane, CodePurpose.DISABLE, jane.toString(), "123456")).thenReturn(CodeCheck.SUCCESS);

        handler.disable(jane, TwoFactorMethod.APP, "123456");

        verify(store).disable(jane, TwoFactorMethod.APP);
        verify(app).forgetPending(jane);
        verify(mails).methodDisabled(jane, TwoFactorMethod.APP, true);
    }

    @Test
    void a_paused_mail_method_is_turned_off_without_a_code() {
        when(store.activeMethods(jane)).thenReturn(EnumSet.of(TwoFactorMethod.MAIL));
        when(mail.usableNow()).thenReturn(false);

        handler.disable(jane, TwoFactorMethod.MAIL, null);

        verify(mail, never()).check(any(), any(), any(), any());
        verify(store).disable(jane, TwoFactorMethod.MAIL);
        verify(mails).methodDisabled(jane, TwoFactorMethod.MAIL, false);
    }

    @Test
    void a_method_that_is_off_cannot_be_turned_off() {
        assertThatThrownBy(() -> handler.disable(jane, TwoFactorMethod.MAIL, "004213"))
            .isInstanceOf(MfaException.MethodNotEnabled.class);
    }

    @Test
    void a_disable_code_spent_by_wrong_guesses_says_to_ask_for_another() {
        // The fifth wrong code takes the code with it: from then on even the right one is refused, and "invalid"
        // would send the person round in circles.
        when(store.activeMethods(jane)).thenReturn(EnumSet.of(TwoFactorMethod.MAIL));
        when(mail.check(jane, CodePurpose.DISABLE, jane.toString(), "000000")).thenReturn(CodeCheck.INVALID);
        when(mail.codePending(jane, CodePurpose.DISABLE)).thenReturn(false);

        assertThatThrownBy(() -> handler.disable(jane, TwoFactorMethod.MAIL, "000000"))
            .isInstanceOf(MfaException.CodeSpent.class);
        verify(store, never()).disable(any(), any());
    }
}
