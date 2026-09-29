package com.nido.api.identity.infrastructure.mail;

import com.nido.api.identity.domain.model.Language;
import com.nido.api.mail.application.port.in.SendMailUseCase;
import com.nido.api.mail.domain.model.MailRequest;
import com.nido.api.mail.domain.model.Recipient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProfileMailAdapterTest {

    @Mock SendMailUseCase sendMail;

    @Test
    void the_old_address_is_told_the_new_one_masked() {
        new ProfileMailAdapter(sendMail).emailChanged("jane", "jane@old.fr", "jane.doe@example.fr", Language.FR);

        ArgumentCaptor<MailRequest> request = ArgumentCaptor.forClass(MailRequest.class);
        verify(sendMail).send(request.capture());
        assertThat(request.getValue().to()).isEqualTo(new Recipient("jane@old.fr", "jane"));
        assertThat(request.getValue().locale()).isEqualTo(Locale.FRENCH);
        assertThat(request.getValue().content()).isEqualTo(new EmailChangedMail("jane", "ja•••@example.fr"));
    }

    @Test
    void masking_keeps_two_letters_one_for_a_short_name_and_the_whole_domain() {
        assertThat(EmailChangedMail.mask("jane.doe@example.fr")).isEqualTo("ja•••@example.fr");
        assertThat(EmailChangedMail.mask("jo@example.fr")).isEqualTo("j•••@example.fr");
        assertThat(EmailChangedMail.mask("j@example.fr")).isEqualTo("j•••@example.fr");
        assertThat(EmailChangedMail.mask("no-at-sign")).isEqualTo("•••");
    }
}
