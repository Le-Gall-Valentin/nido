package com.nido.api.identity.infrastructure.mail;

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
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProfileMailAdapterTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-28T10:00:00Z"), ZoneOffset.UTC);

    @Mock SendMailUseCase sendMail;

    @Test
    void the_old_address_is_told_the_new_one_masked_and_the_alert_expires_a_day_after_it_is_queued() {
        new ProfileMailAdapter(sendMail, CLOCK).emailChanged("jane", "jane@old.fr", "jane.doe@example.fr", Language.FR);

        ArgumentCaptor<MailRequest> request = ArgumentCaptor.forClass(MailRequest.class);
        verify(sendMail).send(request.capture());
        assertThat(request.getValue().to()).isEqualTo(new Recipient("jane@old.fr", "jane"));
        assertThat(request.getValue().locale()).isEqualTo(Locale.FRENCH);
        assertThat(request.getValue().content()).isEqualTo(new EmailChangedMail("jane", "ja•••@example.fr"));
        assertThat(request.getValue().expiresAt()).isEqualTo(Instant.parse("2026-09-29T10:00:00Z"));
    }

    @Test
    void masking_keeps_two_letters_one_for_a_short_name_and_the_whole_domain() {
        assertThat(EmailChangedMail.mask("jane.doe@example.fr")).isEqualTo("ja•••@example.fr");
        assertThat(EmailChangedMail.mask("jo@example.fr")).isEqualTo("j•••@example.fr");
        assertThat(EmailChangedMail.mask("j@example.fr")).isEqualTo("j•••@example.fr");
        assertThat(EmailChangedMail.mask("no-at-sign")).isEqualTo("•••");
    }
}
