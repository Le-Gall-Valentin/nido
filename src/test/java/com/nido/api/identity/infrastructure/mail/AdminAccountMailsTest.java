package com.nido.api.identity.infrastructure.mail;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.mail.domain.model.RenderedMail;
import com.nido.api.mail.infrastructure.render.ThymeleafMailRenderer;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAccountMailsTest {

    private static final ThymeleafMailRenderer RENDERER = new ThymeleafMailRenderer("https://nido.example");
    private static final AppPath LOGIN = new AppPath("/login");

    private static RenderedMail fr(MailContent mail) {
        return RENDERER.render(mail, Locale.FRENCH);
    }

    private static RenderedMail en(MailContent mail) {
        return RENDERER.render(mail, Locale.ENGLISH);
    }

    @Test
    void a_promotion_says_what_it_opens() {
        assertThat(fr(new RoleChangedMail("carol", "alice", true, LOGIN)).subject())
            .isEqualTo("Vous êtes maintenant administrateur de Nido");
        assertThat(fr(new RoleChangedMail("carol", "alice", true, LOGIN)).text())
            .contains("Bonjour carol,", "alice vous a nommé administrateur.", "Se connecter\nhttps://nido.example/login");
        assertThat(en(new RoleChangedMail("carol", "alice", true, LOGIN)).subject())
            .isEqualTo("You are now a Nido administrator");
    }

    @Test
    void a_demotion_says_what_it_takes_away() {
        assertThat(fr(new RoleChangedMail("carol", "alice", false, LOGIN)).subject())
            .isEqualTo("Vous n’êtes plus administrateur de Nido");
        assertThat(fr(new RoleChangedMail("carol", "alice", false, LOGIN)).text())
            .contains("alice vous a retiré le rôle d’administrateur.");
        assertThat(en(new RoleChangedMail("carol", "alice", false, LOGIN)).subject())
            .isEqualTo("You are no longer a Nido administrator");
    }

    @Test
    void a_reset_second_factor_asks_to_turn_it_back_on() {
        TotpResetMail mail = new TotpResetMail("carol", "bob", new AppPath("/account/security"));
        assertThat(fr(mail).subject()).isEqualTo("La double authentification de votre compte Nido a été désactivée");
        assertThat(fr(mail).text()).contains("bob a réinitialisé la double authentification de votre compte.",
            "réactivez-la dès que possible", "https://nido.example/account/security");
        assertThat(en(mail).subject()).isEqualTo("Two-factor authentication was turned off on your Nido account");
    }

    @Test
    void a_deactivation_is_temporary_and_names_who_to_ask() {
        assertThat(fr(new AccountDeactivatedMail("carol", "bob")).subject())
            .isEqualTo("Votre compte Nido est temporairement désactivé");
        assertThat(fr(new AccountDeactivatedMail("carol", "bob")).text())
            .contains("bob a désactivé votre compte Nido.", "Vos données sont conservées", "contactez bob");
        assertThat(en(new AccountDeactivatedMail("carol", "bob")).subject())
            .isEqualTo("Your Nido account is temporarily deactivated");
    }

    @Test
    void a_reactivation_invites_back_in() {
        AccountReactivatedMail mail = new AccountReactivatedMail("carol", "bob", LOGIN);
        assertThat(fr(mail).subject()).isEqualTo("Votre compte Nido est de nouveau actif");
        assertThat(fr(mail).text()).contains("bob a réactivé votre compte Nido.", "https://nido.example/login");
        assertThat(en(mail).subject()).isEqualTo("Your Nido account is active again");
    }

    @Test
    void a_deletion_says_the_data_is_gone() {
        assertThat(fr(new AccountDeletedMail("carol", "bob")).subject()).isEqualTo("Votre compte Nido a été supprimé");
        assertThat(fr(new AccountDeletedMail("carol", "bob")).text())
            .contains("bob a supprimé votre compte Nido. Vos données personnelles ont été effacées");
        assertThat(en(new AccountDeletedMail("carol", "bob")).subject()).isEqualTo("Your Nido account was deleted");
    }

    @Test
    void a_cancelled_invitation_says_the_link_no_longer_works() {
        assertThat(fr(new AccountInvitationCancelledMail("carol", "bob")).subject())
            .isEqualTo("Votre invitation à Nido a été annulée");
        assertThat(fr(new AccountInvitationCancelledMail("carol", "bob")).text())
            .contains("bob a annulé votre invitation à rejoindre Nido. Le lien que vous avez reçu ne fonctionne plus.");
        assertThat(en(new AccountInvitationCancelledMail("carol", "bob")).subject())
            .isEqualTo("Your invitation to Nido was cancelled");
    }

    @Test
    void each_mail_declares_its_template() {
        assertThat(new RoleChangedMail("u", "a", true, LOGIN).template()).isEqualTo("identity/role-changed");
        assertThat(new TotpResetMail("u", "a", LOGIN).template()).isEqualTo("identity/totp-reset");
        assertThat(new AccountDeactivatedMail("u", "a").template()).isEqualTo("identity/account-deactivated");
        assertThat(new AccountReactivatedMail("u", "a", LOGIN).template()).isEqualTo("identity/account-reactivated");
        assertThat(new AccountDeletedMail("u", "a").template()).isEqualTo("identity/account-deleted");
        assertThat(new AccountInvitationCancelledMail("u", "a").template()).isEqualTo("identity/invitation-cancelled");
    }
}
