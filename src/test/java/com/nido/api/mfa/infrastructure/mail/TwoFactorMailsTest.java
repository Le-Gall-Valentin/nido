package com.nido.api.mfa.infrastructure.mail;

import com.nido.api.mail.domain.model.RenderedMail;
import com.nido.api.mail.infrastructure.render.ThymeleafMailRenderer;
import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.shared.model.TwoFactorMethod;
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

    @Test
    void turning_a_method_on_names_it() {
        RenderedMail app = RENDERER.render(new TwoFactorEnabledMail("jane", TwoFactorMethod.APP), Locale.FRENCH);
        RenderedMail mail = RENDERER.render(new TwoFactorEnabledMail("jane", TwoFactorMethod.MAIL), Locale.FRENCH);

        assertThat(app.subject()).isEqualTo("La double authentification est activée sur votre compte Nido");
        assertThat(app.text()).contains("Bonjour jane,", "un code de votre application d’authentification", "contactez un administrateur");
        assertThat(mail.subject()).isEqualTo("Le code par mail est activé sur votre compte Nido");
        assertThat(mail.text()).contains("Nido pourra vous envoyer un code à cette adresse");
    }

    @Test
    void turning_a_method_off_says_what_protects_the_account_now() {
        AppPath security = new AppPath("/account/security");
        RenderedMail alone = RENDERER.render(new TwoFactorDisabledMail("jane", TwoFactorMethod.APP, false, security), Locale.FRENCH);
        RenderedMail backedUp = RENDERER.render(new TwoFactorDisabledMail("jane", TwoFactorMethod.MAIL, true, security), Locale.ENGLISH);

        assertThat(alone.subject()).isEqualTo("L’application d’authentification est désactivée sur votre compte Nido");
        assertThat(alone.text()).contains("n’est plus protégé que par son mot de passe", "https://nido.example/account/security");
        assertThat(backedUp.subject()).isEqualTo("Codes by email are off for your Nido account");
        assertThat(backedUp.text()).contains("stays on with your other method").doesNotContain("password alone");
    }
}
