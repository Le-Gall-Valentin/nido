package com.nido.api.identity.infrastructure.notification;

import com.nido.api.identity.infrastructure.mail.ResetMethods;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.mail.domain.model.RenderedMail;
import com.nido.api.mail.infrastructure.render.ThymeleafMailRenderer;
import com.nido.api.notifications.domain.model.NotificationKind;
import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class AdminActivityMailsTest {

    private static final ThymeleafMailRenderer RENDERER = new ThymeleafMailRenderer("https://nido.example");
    private static final AppPath USERS = new AppPath("/administration/users");

    private static RenderedMail fr(MailContent mail) {
        return RENDERER.render(mail, Locale.FRENCH);
    }

    private static RenderedMail en(MailContent mail) {
        return RENDERER.render(mail, Locale.ENGLISH);
    }

    @Test
    void every_kind_is_reserved_to_super_administrators() {
        List<Class<?>> kinds = List.of(AccountCreatedNotification.class, AccountDeactivatedNotification.class,
            AccountReactivatedNotification.class, AccountDeletedNotification.class, AccountTwoFactorResetNotification.class);

        assertThat(kinds).extracting(kind -> kind.getAnnotation(NotificationKind.class).value()).containsExactly(
            "identity.account-created", "identity.account-deactivated", "identity.account-reactivated",
            "identity.account-deleted", "identity.account-2fa-reset");
        assertThat(kinds).allSatisfy(kind ->
            assertThat(kind.getAnnotation(NotificationKind.class).roles()).containsExactly(Role.SUPER_ADMIN));
    }

    @Test
    void each_mail_says_who_did_what_to_whom_and_links_to_the_accounts() {
        RenderedMail created = fr(new AccountCreatedNotification("alice", "bob", "carol", USERS));
        assertThat(created.subject()).isEqualTo("bob a invité carol");
        assertThat(created.text()).contains("Bonjour alice,", "bob a invité carol à rejoindre Nido.",
            "Voir les comptes\nhttps://nido.example/administration/users");

        assertThat(fr(new AccountDeactivatedNotification("alice", "bob", "carol", USERS)).subject())
            .isEqualTo("bob a désactivé le compte de carol");
        assertThat(fr(new AccountReactivatedNotification("alice", "bob", "carol", USERS)).subject())
            .isEqualTo("bob a réactivé le compte de carol");
        assertThat(fr(new AccountDeletedNotification("alice", "bob", "carol", false, USERS)).subject())
            .isEqualTo("bob a supprimé le compte de carol");
        assertThat(fr(new AccountTwoFactorResetNotification("alice", "bob", "carol", ResetMethods.MAIL, USERS)).subject())
            .isEqualTo("bob a réinitialisé la 2FA de carol");
        assertThat(fr(new AccountTwoFactorResetNotification("alice", "bob", "carol", ResetMethods.MAIL, USERS)).text())
            .contains("bob a retiré le code par mail de carol.");

        assertThat(en(new AccountCreatedNotification("alice", "bob", "carol", USERS)).subject()).isEqualTo("bob invited carol");
        assertThat(en(new AccountDeactivatedNotification("alice", "bob", "carol", USERS)).subject())
            .isEqualTo("bob deactivated carol’s account");
        assertThat(en(new AccountTwoFactorResetNotification("alice", "bob", "carol", ResetMethods.MAIL, USERS)).subject())
            .isEqualTo("bob reset carol’s two-factor authentication");
    }

    @Test
    void deleting_an_account_that_never_joined_is_told_as_a_cancelled_invitation() {
        RenderedMail invited = fr(new AccountDeletedNotification("alice", "bob", "carol", true, USERS));
        assertThat(invited.subject()).isEqualTo("bob a annulé l’invitation de carol");
        assertThat(invited.text())
            .contains("bob a supprimé le compte de carol, qui n’avait pas encore rejoint Nido : son invitation est annulée.")
            .doesNotContain("données personnelles");
        assertThat(en(new AccountDeletedNotification("alice", "bob", "carol", true, USERS)).subject())
            .isEqualTo("bob cancelled carol’s invitation");
        assertThat(fr(new AccountDeletedNotification("alice", "bob", "carol", false, USERS)).text())
            .contains("bob a supprimé le compte de carol et ses données personnelles.");
    }
}
