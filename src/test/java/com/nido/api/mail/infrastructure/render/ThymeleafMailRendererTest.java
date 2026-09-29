package com.nido.api.mail.infrastructure.render;

import com.nido.api.mail.KitSampleMail;
import com.nido.api.mail.NamedTemplateMail;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.RenderedMail;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ThymeleafMailRendererTest {

    private final ThymeleafMailRenderer renderer = new ThymeleafMailRenderer("http://localhost:5173");
    private final KitSampleMail sample = new KitSampleMail("jane", new AppPath("/somewhere#token=abc"));

    @Test
    void writes_the_whole_kit_in_french() {
        RenderedMail mail = renderer.render(sample, Locale.FRENCH);

        assertThat(mail.subject()).isEqualTo("Échantillon du kit");
        assertThat(mail.html())
            .contains("<html lang=\"fr\">")
            .contains("Tous les composants")
            .contains("Bonjour jane,")
            .contains("href=\"http://localhost:5173/somewhere#token=abc\"")
            .contains("src=\"cid:nido-mark\"")
            .contains("#eef2ec")   // the info note
            .contains("#f8e5e1")   // the warning note
            .contains("Nido — toute la vie du foyer, au même endroit.");
        assertThat(mail.text()).isEqualTo("""
            Nido

            Tous les composants

            Bonjour jane,

            Ouvrir Nido
            http://localhost:5173/somewhere#token=abc

            Ce lien est valable 30 minutes.

            Ce n’est pas vous ? Contactez votre administrateur.

            Le bouton ne fonctionne pas ? Copiez ce lien dans votre navigateur :

            http://localhost:5173/somewhere#token=abc

            Petit texte en bas de carte.

            Nido — toute la vie du foyer, au même endroit.

            Vous recevez ce mail parce qu’un compte Nido est associé à cette adresse.
            """);
    }

    @Test
    void writes_the_whole_kit_in_english() {
        RenderedMail mail = renderer.render(sample, Locale.ENGLISH);

        assertThat(mail.subject()).isEqualTo("Kit sample");
        assertThat(mail.html()).contains("<html lang=\"en\">").contains("Every component").contains("Hello jane,");
        assertThat(mail.text()).contains("Open Nido\nhttp://localhost:5173/somewhere#token=abc");
    }

    @Test
    void a_language_the_app_does_not_speak_is_written_in_english_and_a_regional_one_in_its_language() {
        assertThat(renderer.render(sample, Locale.GERMAN).subject()).isEqualTo("Kit sample");
        assertThat(renderer.render(sample, Locale.forLanguageTag("fr-CA")).subject()).isEqualTo("Échantillon du kit");
    }

    @Test
    void text_from_the_caller_is_escaped() {
        RenderedMail mail = renderer.render(new KitSampleMail("<script>alert(1)</script>", sample.link()), Locale.FRENCH);

        assertThat(mail.html()).doesNotContain("<script>").contains("&lt;script&gt;alert(1)&lt;/script&gt;");
        assertThat(mail.text()).contains("Bonjour <script>alert(1)</script>,");
    }

    @Test
    void a_missing_message_is_an_error_not_a_placeholder() {
        assertThatThrownBy(() -> renderer.render(new NamedTemplateMail("test/missing-message"), Locale.FRENCH))
            .rootCause()
            .hasMessageContaining("doesNotExist");
    }

    @Test
    void a_missing_value_is_an_error_not_an_empty_string() {
        assertThatThrownBy(() -> renderer.render(new NamedTemplateMail("test/missing-property"), Locale.FRENCH))
            .rootCause()
            .hasMessageContaining("doesNotExist");
    }

    @Test
    void a_template_without_a_subject_is_an_error() {
        assertThatThrownBy(() -> renderer.render(new NamedTemplateMail("test/no-title"), Locale.FRENCH))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("test/no-title");
    }
}
