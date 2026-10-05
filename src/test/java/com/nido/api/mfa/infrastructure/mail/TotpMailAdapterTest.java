package com.nido.api.mfa.infrastructure.mail;

import com.nido.api.identity.application.port.in.FindUserUseCase;
import com.nido.api.identity.domain.model.User;
import com.nido.api.mail.application.port.in.SendMailUseCase;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailRequest;
import com.nido.api.mail.domain.model.Recipient;
import com.nido.api.shared.model.Language;
import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TotpMailAdapterTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-05T10:00:00Z"), ZoneOffset.UTC);

    @Mock FindUserUseCase findUser;
    @Mock SendMailUseCase sendMail;

    private final UUID janeId = UUID.randomUUID();

    private TotpMailAdapter adapter() {
        return new TotpMailAdapter(findUser, sendMail, CLOCK);
    }

    private MailRequest sent() {
        ArgumentCaptor<MailRequest> request = ArgumentCaptor.forClass(MailRequest.class);
        verify(sendMail).send(request.capture());
        return request.getValue();
    }

    @Test
    void turning_it_on_is_told_to_the_account_in_its_language_for_a_day() {
        when(findUser.findById(janeId)).thenReturn(Optional.of(
            new User(janeId, "jane", "jane@test.com", Role.USER, true, Instant.now(), Language.FR)));

        adapter().totpEnabled(janeId);

        MailRequest request = sent();
        assertThat(request.to()).isEqualTo(new Recipient("jane@test.com", "jane"));
        assertThat(request.locale()).isEqualTo(Locale.FRENCH);
        assertThat(request.expiresAt()).isEqualTo(Instant.parse("2026-10-06T10:00:00Z"));
        assertThat(request.content()).isEqualTo(new TotpEnabledMail("jane"));
    }

    @Test
    void turning_it_off_links_to_the_security_page() {
        when(findUser.findById(janeId)).thenReturn(Optional.of(
            new User(janeId, "jane", "jane@test.com", Role.USER, true, Instant.now(), null)));

        adapter().totpDisabled(janeId);

        assertThat(sent().content()).isEqualTo(new TotpDisabledMail("jane", new AppPath("/account/security")));
    }

    @Test
    void an_account_that_cannot_be_written_to_gets_nothing() {
        when(findUser.findById(janeId)).thenReturn(Optional.empty());

        adapter().totpDisabled(janeId);

        verify(sendMail, never()).send(any());
    }
}
