package com.nido.api.identity.infrastructure.mail;

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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminAccountMailAdapterTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-05T10:00:00Z"), ZoneOffset.UTC);
    private static final Instant A_DAY_LATER = Instant.parse("2026-10-06T10:00:00Z");

    @Mock SendMailUseCase sendMail;

    private final User carol = new User(UUID.randomUUID(), "carol", "carol@test.com", Role.USER, true,
        Instant.parse("2026-01-01T00:00:00Z"), Language.FR);

    private AdminAccountMailAdapter adapter() {
        return new AdminAccountMailAdapter(sendMail, CLOCK);
    }

    private MailRequest sent() {
        ArgumentCaptor<MailRequest> request = ArgumentCaptor.forClass(MailRequest.class);
        verify(sendMail).send(request.capture());
        return request.getValue();
    }

    @Test
    void an_alert_goes_to_the_account_in_its_language_and_expires_a_day_after_it_is_queued() {
        adapter().deactivated(carol, "bob");

        MailRequest request = sent();
        assertThat(request.to()).isEqualTo(new Recipient("carol@test.com", "carol"));
        assertThat(request.locale()).isEqualTo(Locale.FRENCH);
        assertThat(request.expiresAt()).isEqualTo(A_DAY_LATER);
        assertThat(request.content()).isEqualTo(new AccountDeactivatedMail("carol", "bob"));
    }

    @Test
    void a_promotion_and_a_demotion_are_told_apart() {
        adapter().roleChanged(carol, "alice", Role.ADMIN);
        assertThat(sent().content()).isEqualTo(new RoleChangedMail("carol", "alice", true, new AppPath("/login")));
    }

    @Test
    void each_gesture_has_its_mail() {
        adapter().totpReset(carol, "bob");
        assertThat(sent().content()).isEqualTo(new TotpResetMail("carol", "bob", new AppPath("/account/security")));
    }

    @Test
    void the_other_gestures_have_theirs() {
        AdminAccountMailAdapter adapter = adapter();
        ArgumentCaptor<MailRequest> requests = ArgumentCaptor.forClass(MailRequest.class);

        adapter.reactivated(carol, "bob");
        adapter.deleted(carol, "bob");
        adapter.invitationCancelled(carol, "bob");

        verify(sendMail, times(3)).send(requests.capture());
        assertThat(requests.getAllValues()).extracting(MailRequest::content).containsExactly(
            new AccountReactivatedMail("carol", "bob", new AppPath("/login")),
            new AccountDeletedMail("carol", "bob"),
            new AccountInvitationCancelledMail("carol", "bob"));
    }

    @Test
    void an_account_without_an_address_gets_nothing_and_breaks_nothing() {
        adapter().deleted(new User(UUID.randomUUID(), "ghost", null, Role.USER, true, Instant.now(), null), "bob");

        verify(sendMail, never()).send(any());
    }
}
