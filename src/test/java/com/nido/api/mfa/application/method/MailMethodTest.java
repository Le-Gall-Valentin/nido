package com.nido.api.mfa.application.method;

import com.nido.api.mfa.domain.model.CodeCheck;
import com.nido.api.mfa.domain.model.CodeDelivery;
import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.mfa.domain.model.EnrolmentStarted;
import com.nido.api.mfa.domain.model.MfaException;
import com.nido.api.mfa.domain.port.out.AccountAddressPort;
import com.nido.api.mfa.domain.port.out.MailAvailabilityPort;
import com.nido.api.mfa.domain.port.out.MailCodeHasherPort;
import com.nido.api.mfa.domain.port.out.MailCodeSendLimitPort;
import com.nido.api.mfa.domain.port.out.TwoFactorMailPort;
import com.nido.api.mfa.infrastructure.security.HmacMailCodeHasherAdapter;
import com.nido.api.shared.model.TwoFactorMethod;
import com.nido.api.shared.security.EncryptionKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailMethodTest {

    private static final Instant START = Instant.parse("2026-10-07T10:00:00Z");

    private final UUID jane = UUID.randomUUID();
    private final InMemoryMailCodeStore codes = new InMemoryMailCodeStore();
    private final MailCodeSendLimitPort limit = mock(MailCodeSendLimitPort.class);
    private final TwoFactorMailPort mails = mock(TwoFactorMailPort.class);
    private final MailAvailabilityPort availability = mock(MailAvailabilityPort.class);
    private final AccountAddressPort addresses = mock(AccountAddressPort.class);
    private final MailCodeHasherPort hasher = new HmacMailCodeHasherAdapter(new EncryptionKey("test-installation-key"));
    private final Deque<String> nextCodes = new ArrayDeque<>(List.of("004213", "771205", "555555"));

    private Instant now = START;
    private MailMethod mail;

    @BeforeEach
    void setUp() {
        Clock clock = new Clock() {
            @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return now; }
        };
        when(availability.isAvailable()).thenReturn(true);
        when(limit.tryCount(jane)).thenReturn(OptionalLong.empty());
        when(addresses.addressOf(jane)).thenReturn(Optional.of("jane@example.fr"));
        MailCodeIssuer issuer = new MailCodeIssuer(codes, limit, nextCodes::pop, hasher, mails, availability, clock);
        mail = new MailMethod(issuer, availability, addresses);
    }

    private String lastCodeSent() {
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(mails, org.mockito.Mockito.atLeastOnce()).sendCode(eq(jane), anyString(), any(), code.capture(), any());
        return code.getValue();
    }

    @Test
    void the_mail_method_sends_codes_and_is_usable_while_mail_is_on() {
        assertThat(mail.method()).isEqualTo(TwoFactorMethod.MAIL);
        assertThat(mail).isInstanceOf(CodeSendingMethod.class);
        assertThat(mail.usableNow()).isTrue();
        when(availability.isAvailable()).thenReturn(false);
        assertThat(mail.usableNow()).isFalse();
    }

    @Test
    void turning_it_on_sends_a_code_to_the_current_address_that_lives_ten_minutes() {
        EnrolmentStarted started = mail.startEnrolment(jane);

        assertThat(started).isEqualTo(new EnrolmentStarted.MailEnrolment("jane@example.fr", 60));
        verify(mails).sendCode(jane, "jane@example.fr", CodePurpose.ENROL, "004213", START.plusSeconds(600));
    }

    @Test
    void nothing_is_sent_while_mail_is_off() {
        when(availability.isAvailable()).thenReturn(false);

        assertThatThrownBy(() -> mail.startEnrolment(jane)).isInstanceOf(MfaException.MethodUnavailable.class);
        assertThat(mail.sendCode(jane, CodePurpose.LOGIN, "c1")).isEqualTo(new CodeDelivery.Unavailable());
        verify(mails, never()).sendCode(any(), any(), any(), any(), any());
    }

    @Test
    void the_code_is_never_kept_in_clear() {
        mail.sendCode(jane, CodePurpose.LOGIN, "c1");

        assertThat(codes.codes.values()).singleElement().satisfies(kept -> {
            assertThat(kept.codeHash()).doesNotContain("004213");
            assertThat(kept.bindingHash()).doesNotContain("c1");
        });
    }

    @Test
    void a_resend_for_the_same_challenge_waits_a_minute_then_replaces_the_code() {
        assertThat(mail.sendCode(jane, CodePurpose.LOGIN, "c1")).isEqualTo(new CodeDelivery.Sent(60));
        now = START.plusSeconds(20);

        assertThat(mail.sendCode(jane, CodePurpose.LOGIN, "c1")).isEqualTo(new CodeDelivery.TooSoon(40));

        now = START.plusSeconds(61);
        assertThat(mail.sendCode(jane, CodePurpose.LOGIN, "c1")).isEqualTo(new CodeDelivery.Sent(60));
        assertThat(mail.check(jane, CodePurpose.LOGIN, "c1", "004213")).isEqualTo(CodeCheck.INVALID);
        assertThat(mail.check(jane, CodePurpose.LOGIN, "c1", "771205")).isEqualTo(CodeCheck.SUCCESS);
    }

    @Test
    void a_send_holds_the_code_before_reading_it_so_two_at_once_queue_up() {
        // Read before the first had written, the second of two sends at once would mail a code of its own and
        // leave the first one dead in the mailbox.
        mail.sendCode(jane, CodePurpose.LOGIN, "c1");

        assertThat(codes.calls).startsWith("lock LOGIN", "find LOGIN");
    }

    @Test
    void a_second_login_replaces_the_first_code_at_once() {
        mail.sendCode(jane, CodePurpose.LOGIN, "first-tab");
        now = START.plusSeconds(5);

        assertThat(mail.sendCode(jane, CodePurpose.LOGIN, "second-tab")).isEqualTo(new CodeDelivery.Sent(60));

        assertThat(mail.check(jane, CodePurpose.LOGIN, "first-tab", "004213"))
            .as("nothing waits for the first tab any more").isEqualTo(CodeCheck.EXPIRED);
        assertThat(mail.check(jane, CodePurpose.LOGIN, "second-tab", "771205")).isEqualTo(CodeCheck.SUCCESS);
    }

    @Test
    void the_account_limit_refuses_with_the_wait() {
        when(limit.tryCount(jane)).thenReturn(OptionalLong.of(420));

        assertThat(mail.sendCode(jane, CodePurpose.LOGIN, "c1")).isEqualTo(new CodeDelivery.LimitReached(420));
        verify(mails, never()).sendCode(any(), any(), any(), any(), any());
    }

    @Test
    void a_code_works_once_and_not_after_ten_minutes() {
        mail.sendCode(jane, CodePurpose.LOGIN, "c1");
        assertThat(mail.check(jane, CodePurpose.LOGIN, "c1", "004213")).isEqualTo(CodeCheck.SUCCESS);
        assertThat(mail.check(jane, CodePurpose.LOGIN, "c1", "004213")).isEqualTo(CodeCheck.EXPIRED);

        now = START.plusSeconds(120);
        mail.sendCode(jane, CodePurpose.LOGIN, "c2");
        now = START.plusSeconds(120 + 600);
        assertThat(mail.check(jane, CodePurpose.LOGIN, "c2", "771205")).isEqualTo(CodeCheck.EXPIRED);
    }

    @Test
    void a_code_that_expired_or_was_never_asked_for_is_not_counted_as_a_wrong_guess() {
        // Saying "too many wrong codes" to someone who only waited too long would have them fear an attack.
        assertThat(mail.check(jane, CodePurpose.DISABLE, jane.toString(), "004213")).isEqualTo(CodeCheck.EXPIRED);

        mail.sendCode(jane, CodePurpose.DISABLE, jane.toString());
        now = START.plusSeconds(601);

        assertThat(mail.check(jane, CodePurpose.DISABLE, jane.toString(), "004213")).isEqualTo(CodeCheck.EXPIRED);
        assertThat(codes.codes.values()).singleElement().extracting("failedAttempts").isEqualTo(0);
    }

    @Test
    void the_fifth_wrong_code_spends_it_and_the_right_one_then_finds_nothing() {
        mail.sendCode(jane, CodePurpose.DISABLE, jane.toString());
        for (int i = 0; i < 4; i++) {
            assertThat(mail.check(jane, CodePurpose.DISABLE, jane.toString(), "999999")).isEqualTo(CodeCheck.INVALID);
        }

        assertThat(mail.check(jane, CodePurpose.DISABLE, jane.toString(), "999999")).isEqualTo(CodeCheck.SPENT);
        assertThat(mail.check(jane, CodePurpose.DISABLE, jane.toString(), "004213")).isEqualTo(CodeCheck.EXPIRED);
    }

    @Test
    void of_two_requests_carrying_the_right_code_at_once_only_one_gets_in() {
        mail.sendCode(jane, CodePurpose.LOGIN, "c1");
        codes.takenRightAfterNextFind = true;

        assertThat(mail.check(jane, CodePurpose.LOGIN, "c1", "004213")).isEqualTo(CodeCheck.EXPIRED);
    }

    @Test
    void wrong_codes_at_sign_in_are_counted_by_the_account_not_by_the_code() {
        mail.sendCode(jane, CodePurpose.LOGIN, "c1");
        for (int i = 0; i < 6; i++) {
            assertThat(mail.check(jane, CodePurpose.LOGIN, "c1", "999999")).isEqualTo(CodeCheck.INVALID);
        }

        assertThat(mail.check(jane, CodePurpose.LOGIN, "c1", "004213")).isEqualTo(CodeCheck.SUCCESS);
    }

    @Test
    void confirming_the_address_needs_a_code_under_way_and_five_wrong_ones_end_it() {
        assertThatThrownBy(() -> mail.confirmEnrolment(jane, "004213")).isInstanceOf(MfaException.EnrolmentNotStarted.class);

        mail.startEnrolment(jane);
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> mail.confirmEnrolment(jane, "999999")).isInstanceOf(MfaException.CodeInvalid.class);
        }
        assertThatThrownBy(() -> mail.confirmEnrolment(jane, "999999"))
            .isInstanceOf(MfaException.ConfirmMaxAttemptsExceeded.class);
        assertThatThrownBy(() -> mail.confirmEnrolment(jane, "004213")).isInstanceOf(MfaException.EnrolmentNotStarted.class);
    }

    @Test
    void the_right_code_confirms_the_address_and_keeps_no_secret() {
        mail.startEnrolment(jane);

        assertThat(mail.confirmEnrolment(jane, lastCodeSent())).isEmpty();
    }

    @Test
    void forgetting_drops_every_code_of_the_account() {
        mail.sendCode(jane, CodePurpose.LOGIN, "c1");
        mail.sendCode(jane, CodePurpose.DISABLE, jane.toString());

        mail.forgetPending(jane);

        assertThat(codes.codes).isEmpty();
        verify(mails, times(2)).sendCode(eq(jane), anyString(), any(), anyString(), any());
    }

    @Test
    void an_account_without_an_address_gets_no_code() {
        when(addresses.addressOf(jane)).thenReturn(Optional.empty());

        assertThat(mail.sendCode(jane, CodePurpose.LOGIN, "c1")).isEqualTo(new CodeDelivery.Unavailable());
        assertThatThrownBy(() -> mail.startEnrolment(jane)).isInstanceOf(MfaException.UserNotFound.class);
    }
}
