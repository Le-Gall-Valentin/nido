package com.nido.api.mfa.infrastructure.mail;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.RenderedMail;
import com.nido.api.mail.infrastructure.render.ThymeleafMailRenderer;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class TotpMailsTest {

    private static final ThymeleafMailRenderer RENDERER = new ThymeleafMailRenderer("https://nido.example");

    @Test
    void turned_on_says_a_code_will_be_asked_and_what_to_do_if_it_was_not_you() {
        RenderedMail fr = RENDERER.render(new TotpEnabledMail("jane"), Locale.FRENCH);
        RenderedMail en = RENDERER.render(new TotpEnabledMail("jane"), Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("La double authentification est activée sur votre compte Nido");
        assertThat(fr.text()).contains("Bonjour jane,", "Nido vous demandera un code", "contactez un administrateur");
        assertThat(en.subject()).isEqualTo("Two-factor authentication is on for your Nido account");
    }

    @Test
    void turned_off_says_the_password_alone_protects_the_account_and_links_to_security() {
        TotpDisabledMail mail = new TotpDisabledMail("jane", new AppPath("/account/security"));
        RenderedMail fr = RENDERER.render(mail, Locale.FRENCH);
        RenderedMail en = RENDERER.render(mail, Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("La double authentification est désactivée sur votre compte Nido");
        assertThat(fr.text()).contains("n’est plus protégé que par son mot de passe",
            "https://nido.example/account/security");
        assertThat(en.subject()).isEqualTo("Two-factor authentication is off for your Nido account");
    }

    @Test
    void each_mail_declares_its_template() {
        assertThat(new TotpEnabledMail("u").template()).isEqualTo("mfa/totp-enabled");
        assertThat(new TotpDisabledMail("u", new AppPath("/x")).template()).isEqualTo("mfa/totp-disabled");
    }
}
