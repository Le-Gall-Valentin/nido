package com.nido.api.authentication.infrastructure.mail;

import com.nido.api.authentication.domain.model.AccountContact;
import com.nido.api.mail.application.port.in.MailAvailabilityQuery;
import com.nido.api.mail.application.port.in.SendMailUseCase;
import com.nido.api.mail.domain.model.MailRequest;
import com.nido.api.mail.domain.model.Recipient;
import com.nido.api.shared.model.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountMailAdapterTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-28T10:00:00Z"), ZoneOffset.UTC);

    @Mock SendMailUseCase sendMail;
    @Mock MailAvailabilityQuery availability;

    private final AccountContact jane = new AccountContact(UUID.randomUUID(), "jane", "jane@test.com", Language.EN);

    private MailRequest sent() {
        ArgumentCaptor<MailRequest> request = ArgumentCaptor.forClass(MailRequest.class);
        verify(sendMail).send(request.capture());
        return request.getValue();
    }

    @Test
    void a_reset_mail_carries_the_link_path_and_dies_with_the_token() {
        Instant expiresAt = Instant.parse("2026-09-28T10:30:00Z");

        new AccountMailAdapter(sendMail, availability, CLOCK)
            .passwordResetRequested(jane, "RAW-token_1", expiresAt, Duration.ofMinutes(30));

        MailRequest request = sent();
        assertThat(request.to()).isEqualTo(new Recipient("jane@test.com", "jane"));
        assertThat(request.locale()).isEqualTo(Locale.ENGLISH);
        assertThat(request.expiresAt()).isEqualTo(expiresAt);
        assertThat(request.content()).isInstanceOfSatisfying(PasswordResetMail.class, mail -> {
            assertThat(mail.username()).isEqualTo("jane");
            assertThat(mail.resetPath().value()).isEqualTo("/reset-password#token=RAW-token_1");
            assertThat(mail.validityMinutes()).isEqualTo(30);
        });
    }

    @Test
    void a_password_changed_mail_links_to_the_login_page_and_expires_a_day_after_it_is_queued() {
        new AccountMailAdapter(sendMail, availability, CLOCK).passwordChanged(jane);

        MailRequest request = sent();
        assertThat(request.expiresAt()).isEqualTo(Instant.parse("2026-09-29T10:00:00Z"));
        assertThat(request.content()).isInstanceOfSatisfying(PasswordChangedMail.class,
            mail -> assertThat(mail.loginPath().value()).isEqualTo("/login"));
    }

    @Test
    void an_account_without_an_address_gets_no_mail_and_breaks_nothing() {
        new AccountMailAdapter(sendMail, availability, CLOCK)
            .passwordChanged(new AccountContact(UUID.randomUUID(), "ghost", null, null));

        verify(sendMail, never()).send(any());
    }

    @Test
    void it_can_send_exactly_when_mail_is_on() {
        when(availability.isAvailable()).thenReturn(true);

        assertThat(new AccountMailAdapter(sendMail, availability, CLOCK).canSend()).isTrue();
    }
}
