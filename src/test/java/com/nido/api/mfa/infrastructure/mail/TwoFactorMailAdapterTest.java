package com.nido.api.mfa.infrastructure.mail;

import com.nido.api.identity.application.port.in.FindUserUseCase;
import com.nido.api.identity.domain.model.User;
import com.nido.api.mail.application.port.in.SendMailUseCase;
import com.nido.api.mail.domain.model.MailRequest;
import com.nido.api.mail.domain.model.Recipient;
import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.shared.model.Language;
import com.nido.api.shared.model.Role;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.shared.model.TwoFactorMethod;
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
class TwoFactorMailAdapterTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T10:00:00Z"), ZoneOffset.UTC);
    private static final Instant EXPIRY = Instant.parse("2026-10-07T10:10:00Z");

    @Mock FindUserUseCase findUser;
    @Mock SendMailUseCase sendMail;

    private final UUID janeId = UUID.randomUUID();

    private TwoFactorMailAdapter adapter() {
        return new TwoFactorMailAdapter(findUser, sendMail, CLOCK);
    }

    private MailRequest sent() {
        ArgumentCaptor<MailRequest> request = ArgumentCaptor.forClass(MailRequest.class);
        verify(sendMail).send(request.capture());
        return request.getValue();
    }

    @Test
    void a_code_goes_to_the_given_address_in_the_account_language_and_dies_with_the_code() {
        when(findUser.findById(janeId)).thenReturn(Optional.of(
            new User(janeId, "jane", "jane@old.fr", Role.USER, true, Instant.now(), Language.FR)));

        adapter().sendCode(janeId, "jane@new.fr", CodePurpose.EMAIL_CHANGE, "004213", EXPIRY);

        MailRequest request = sent();
        assertThat(request.to()).isEqualTo(new Recipient("jane@new.fr", "jane"));
        assertThat(request.locale()).isEqualTo(Locale.FRENCH);
        assertThat(request.expiresAt()).isEqualTo(EXPIRY);
        assertThat(request.content()).isEqualTo(new TwoFactorCodeMail("jane", "004213", CodePurpose.EMAIL_CHANGE));
    }

    @Test
    void an_unknown_account_gets_nothing() {
        when(findUser.findById(janeId)).thenReturn(Optional.empty());

        adapter().sendCode(janeId, "jane@new.fr", CodePurpose.LOGIN, "004213", EXPIRY);

        verify(sendMail, never()).send(any());
    }

    @Test
    void turning_a_method_on_is_told_to_the_account_for_a_day() {
        when(findUser.findById(janeId)).thenReturn(Optional.of(
            new User(janeId, "jane", "jane@test.com", Role.USER, true, Instant.now(), Language.FR)));

        adapter().methodEnabled(janeId, TwoFactorMethod.MAIL);

        MailRequest request = sent();
        assertThat(request.to()).isEqualTo(new Recipient("jane@test.com", "jane"));
        assertThat(request.expiresAt()).isEqualTo(Instant.parse("2026-10-08T10:00:00Z"));
        assertThat(request.content()).isEqualTo(new TwoFactorEnabledMail("jane", TwoFactorMethod.MAIL));
    }

    @Test
    void turning_a_method_off_links_to_the_security_page() {
        when(findUser.findById(janeId)).thenReturn(Optional.of(
            new User(janeId, "jane", "jane@test.com", Role.USER, true, Instant.now(), null)));

        adapter().methodDisabled(janeId, TwoFactorMethod.APP, true);

        assertThat(sent().content())
            .isEqualTo(new TwoFactorDisabledMail("jane", TwoFactorMethod.APP, true, new AppPath("/account/security")));
    }
}
