package com.nido.api.mfa.infrastructure.mail;

import com.nido.api.mail.domain.model.RenderedMail;
import com.nido.api.mail.infrastructure.render.ThymeleafMailRenderer;
import com.nido.api.mfa.domain.model.CodePurpose;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class TwoFactorMailsTest {

    private static final ThymeleafMailRenderer RENDERER = new ThymeleafMailRenderer("https://nido.example");

    @Test
    void the_sign_in_code_is_in_the_mail_but_not_in_its_subject() {
        RenderedMail fr = RENDERER.render(new TwoFactorCodeMail("jane", "004213", CodePurpose.LOGIN), Locale.FRENCH);

        assertThat(fr.subject()).isEqualTo("Votre code de connexion Nido").doesNotContain("004213");
        assertThat(fr.text()).contains("Bonjour jane,", "004213", "Il expire dans 10 minutes",
            "Quelqu’un connaît votre mot de passe");
    }

    @Test
    void each_purpose_says_what_the_code_is_for() {
        assertThat(RENDERER.render(new TwoFactorCodeMail("jane", "004213", CodePurpose.ENROL), Locale.FRENCH).subject())
            .isEqualTo("Confirmez votre adresse pour la double authentification");
        assertThat(RENDERER.render(new TwoFactorCodeMail("jane", "004213", CodePurpose.DISABLE), Locale.FRENCH).subject())
            .isEqualTo("Code pour désactiver la double authentification par mail");
        RenderedMail change = RENDERER.render(new TwoFactorCodeMail("jane", "004213", CodePurpose.EMAIL_CHANGE), Locale.ENGLISH);
        assertThat(change.subject()).isEqualTo("Confirm your new Nido address");
        assertThat(change.text()).contains("nothing changes until this code is entered");
    }

    @Test
    void the_mail_declares_its_template() {
        assertThat(new TwoFactorCodeMail("u", "000000", CodePurpose.LOGIN).template()).isEqualTo("mfa/two-factor-code");
    }
}
