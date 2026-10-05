package com.nido.api.authentication.infrastructure.mail;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.RenderedMail;
import com.nido.api.mail.infrastructure.render.ThymeleafMailRenderer;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class AccountInvitationMailsTest {

    private static final ThymeleafMailRenderer RENDERER = new ThymeleafMailRenderer("https://nido.example");
    private static final AppPath WELCOME = new AppPath("/welcome#token=abc");

    @Test
    void an_invitation_names_who_invited_gives_the_identifier_and_the_link() {
        RenderedMail fr = RENDERER.render(new AccountInvitationMail("carol", "bob", WELCOME, 7), Locale.FRENCH);
        RenderedMail en = RENDERER.render(new AccountInvitationMail("carol", "bob", WELCOME, 7), Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("bob vous invite à rejoindre Nido");
        assertThat(fr.text()).contains("Bonjour carol,", "bob vous invite à rejoindre Nido",
            "Votre identifiant : carol", "Choisir mon mot de passe\nhttps://nido.example/welcome#token=abc",
            "Ce lien est valable 7 jours");
        assertThat(en.subject()).isEqualTo("bob invites you to join Nido");
        assertThat(en.text()).contains("Hello carol,", "Your username: carol", "Choose my password",
            "This link is valid for 7 days");
    }

    @Test
    void a_renewed_invitation_only_offers_the_new_link() {
        RenderedMail fr = RENDERER.render(new InvitationRenewedMail("carol", WELCOME, 7), Locale.FRENCH);
        RenderedMail en = RENDERER.render(new InvitationRenewedMail("carol", WELCOME, 7), Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("Votre nouveau lien pour rejoindre Nido");
        assertThat(fr.text()).contains("Votre identifiant : carol", "https://nido.example/welcome#token=abc");
        assertThat(en.subject()).isEqualTo("Your new link to join Nido");
    }

    @Test
    void each_mail_declares_its_template() {
        assertThat(new AccountInvitationMail("u", "i", WELCOME, 7).template()).isEqualTo("authentication/account-invitation");
        assertThat(new InvitationRenewedMail("u", WELCOME, 7).template()).isEqualTo("authentication/invitation-renewed");
    }
}
