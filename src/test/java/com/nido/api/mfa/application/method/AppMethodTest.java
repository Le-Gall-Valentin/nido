package com.nido.api.mfa.application.method;

import com.nido.api.mfa.domain.model.CodeCheck;
import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.mfa.domain.model.EnrolmentStarted;
import com.nido.api.mfa.domain.model.MfaException;
import com.nido.api.mfa.domain.port.out.AccountAddressPort;
import com.nido.api.mfa.domain.port.out.PendingTotpEnrolmentPort;
import com.nido.api.mfa.domain.port.out.TotpCodeReplayPort;
import com.nido.api.mfa.domain.port.out.TotpCodeValidatorPort;
import com.nido.api.mfa.domain.port.out.TotpAttemptPort;
import com.nido.api.mfa.domain.port.out.TotpSecretGeneratorPort;
import com.nido.api.mfa.domain.port.out.TotpUriBuilderPort;
import com.nido.api.mfa.domain.port.out.TwoFactorMethodStorePort;
import com.nido.api.shared.model.TwoFactorMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppMethodTest {

    @Mock TwoFactorMethodStorePort store;
    @Mock TotpSecretGeneratorPort secrets;
    @Mock TotpUriBuilderPort uris;
    @Mock PendingTotpEnrolmentPort pending;
    @Mock TotpCodeValidatorPort validator;
    @Mock TotpCodeReplayPort replay;
    @Mock TotpAttemptPort attempts;
    @Mock AccountAddressPort addresses;

    private AppMethod app;
    private final UUID jane = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        app = new AppMethod(store, secrets, uris, pending, validator, replay, attempts, addresses);
    }

    @Test
    void the_app_is_always_usable_and_sends_nothing() {
        assertThat(app.method()).isEqualTo(TwoFactorMethod.APP);
        assertThat(app.usableNow()).isTrue();
        assertThat(app).isNotInstanceOf(CodeSendingMethod.class);
    }

    @Test
    void an_enrolment_shows_a_new_secret_labelled_with_the_current_address() {
        when(addresses.addressOf(jane)).thenReturn(Optional.of("jane@example.fr"));
        when(secrets.generateSecret()).thenReturn("NEWSECRET");
        when(pending.startIfAbsent(jane, "NEWSECRET")).thenReturn(true);
        when(uris.buildOtpauthUri("NEWSECRET", "jane@example.fr")).thenReturn("otpauth://new");

        assertThat(app.startEnrolment(jane)).isEqualTo(new EnrolmentStarted.AppEnrolment("NEWSECRET", "otpauth://new"));
    }

    @Test
    void a_second_tab_is_shown_the_enrolment_already_under_way() {
        when(addresses.addressOf(jane)).thenReturn(Optional.of("jane@example.fr"));
        when(secrets.generateSecret()).thenReturn("OTHER");
        when(pending.startIfAbsent(jane, "OTHER")).thenReturn(false);
        when(pending.find(jane)).thenReturn(Optional.of("FIRST"));
        when(uris.buildOtpauthUri("FIRST", "jane@example.fr")).thenReturn("otpauth://first");

        assertThat(app.startEnrolment(jane)).isEqualTo(new EnrolmentStarted.AppEnrolment("FIRST", "otpauth://first"));
    }

    @Test
    void an_account_without_an_address_cannot_enrol() {
        when(addresses.addressOf(jane)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> app.startEnrolment(jane)).isInstanceOf(MfaException.UserNotFound.class);
    }

    @Test
    void a_good_first_code_hands_back_the_secret_and_leaves_the_enrolment_for_the_caller_to_forget() {
        when(pending.find(jane)).thenReturn(Optional.of("SECRET"));
        when(validator.isValid("SECRET", "123456")).thenReturn(true);
        when(replay.markCodeUsedIfAbsent(jane, "123456")).thenReturn(true);

        assertThat(app.confirmEnrolment(jane, "123456")).contains("SECRET");
        verify(attempts).clear(jane, CodePurpose.ENROL);
        verify(pending, never()).discard(jane);
    }

    @Test
    void nothing_to_confirm_is_said_once_for_never_started_and_expired() {
        when(pending.find(jane)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> app.confirmEnrolment(jane, "123456")).isInstanceOf(MfaException.EnrolmentNotStarted.class);
    }

    @Test
    void a_wrong_first_code_counts_and_the_fifth_abandons_the_enrolment() {
        when(pending.find(jane)).thenReturn(Optional.of("SECRET"));
        when(validator.isValid("SECRET", "000000")).thenReturn(false);
        when(attempts.recordFailure(jane, CodePurpose.ENROL)).thenReturn(4, 5);

        assertThatThrownBy(() -> app.confirmEnrolment(jane, "000000")).isInstanceOf(MfaException.CodeInvalid.class);
        assertThatThrownBy(() -> app.confirmEnrolment(jane, "000000"))
            .isInstanceOf(MfaException.ConfirmMaxAttemptsExceeded.class);
        verify(pending).discard(jane);
        verify(attempts).clear(jane, CodePurpose.ENROL);
    }

    @Test
    void a_first_code_already_used_is_refused() {
        when(pending.find(jane)).thenReturn(Optional.of("SECRET"));
        when(validator.isValid("SECRET", "123456")).thenReturn(true);
        when(replay.markCodeUsedIfAbsent(jane, "123456")).thenReturn(false);

        assertThatThrownBy(() -> app.confirmEnrolment(jane, "123456")).isInstanceOf(MfaException.CodeInvalid.class);
    }

    @Test
    void a_code_is_checked_against_the_stored_secret_once() {
        when(store.appSecret(jane)).thenReturn(Optional.of("SECRET"));
        when(validator.isValid("SECRET", "123456")).thenReturn(true);
        when(replay.markCodeUsedIfAbsent(jane, "123456")).thenReturn(true, false);

        assertThat(app.check(jane, CodePurpose.LOGIN, "challenge", "123456")).isEqualTo(CodeCheck.SUCCESS);
        assertThat(app.check(jane, CodePurpose.LOGIN, "challenge", "123456")).isEqualTo(CodeCheck.REPLAYED);
    }

    @Test
    void a_wrong_code_or_no_secret_is_invalid() {
        when(store.appSecret(jane)).thenReturn(Optional.of("SECRET"), Optional.empty());
        when(validator.isValid("SECRET", "000000")).thenReturn(false);
        when(attempts.recordFailure(jane, CodePurpose.DISABLE)).thenReturn(1);

        assertThat(app.check(jane, CodePurpose.DISABLE, jane.toString(), "000000")).isEqualTo(CodeCheck.INVALID);
        assertThat(app.check(jane, CodePurpose.LOGIN, "challenge", "123456")).isEqualTo(CodeCheck.INVALID);
    }

    @Test
    void wrong_codes_at_sign_in_are_left_to_the_account_counter() {
        when(store.appSecret(jane)).thenReturn(Optional.of("SECRET"));
        when(validator.isValid("SECRET", "000000")).thenReturn(false);

        app.check(jane, CodePurpose.LOGIN, "challenge", "000000");

        verify(attempts, never()).recordFailure(any(), any());
    }

    @Test
    void wrong_codes_to_turn_the_app_off_are_counted_and_the_fifth_spends_the_way_for_a_while() {
        // Without a count, a borrowed session could guess its way to turning the app off, at the pace of the route alone.
        when(store.appSecret(jane)).thenReturn(Optional.of("SECRET"));
        when(validator.isValid("SECRET", "000000")).thenReturn(false);
        when(attempts.recordFailure(jane, CodePurpose.DISABLE)).thenReturn(4, 5);

        assertThat(app.check(jane, CodePurpose.DISABLE, jane.toString(), "000000")).isEqualTo(CodeCheck.INVALID);
        assertThat(app.check(jane, CodePurpose.DISABLE, jane.toString(), "000000")).isEqualTo(CodeCheck.SPENT);
    }

    @Test
    void once_spent_even_the_right_code_is_refused_unread_and_uncounted() {
        when(attempts.failures(jane, CodePurpose.DISABLE)).thenReturn(5);

        assertThat(app.check(jane, CodePurpose.DISABLE, jane.toString(), "123456")).isEqualTo(CodeCheck.SPENT);
        verify(validator, never()).isValid(any(), any());
        verify(attempts, never()).recordFailure(any(), any());
    }

    @Test
    void the_right_code_to_turn_it_off_clears_the_count() {
        when(store.appSecret(jane)).thenReturn(Optional.of("SECRET"));
        when(attempts.failures(jane, CodePurpose.DISABLE)).thenReturn(2);
        when(validator.isValid("SECRET", "123456")).thenReturn(true);
        when(replay.markCodeUsedIfAbsent(jane, "123456")).thenReturn(true);

        assertThat(app.check(jane, CodePurpose.DISABLE, jane.toString(), "123456")).isEqualTo(CodeCheck.SUCCESS);
        verify(attempts).clear(jane, CodePurpose.DISABLE);
    }

    @Test
    void forgetting_drops_the_enrolment_and_every_count() {
        app.forgetPending(jane);

        verify(pending).discard(jane);
        verify(attempts).clear(jane, CodePurpose.ENROL);
        verify(attempts).clear(jane, CodePurpose.DISABLE);
    }
}
