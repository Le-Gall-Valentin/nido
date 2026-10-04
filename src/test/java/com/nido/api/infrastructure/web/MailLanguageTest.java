package com.nido.api.infrastructure.web;

import com.nido.api.shared.model.Language;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class MailLanguageTest {

    @AfterEach
    void forgetTheRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static void requestAsking(String acceptLanguage) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (acceptLanguage != null) {
            request.addHeader("Accept-Language", acceptLanguage);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void the_account_language_wins_over_the_request() {
        requestAsking("en");

        assertThat(MailLanguage.resolve(Language.FR)).isEqualTo(Locale.FRENCH);
    }

    @Test
    void without_an_account_language_the_request_decides() {
        requestAsking("fr-FR,fr;q=0.9,en;q=0.8");

        assertThat(MailLanguage.resolve(null)).isEqualTo(Locale.FRENCH);
    }

    @Test
    void a_language_the_app_does_not_speak_falls_back_to_english() {
        requestAsking("de-DE,de;q=0.9");

        assertThat(MailLanguage.resolve(null)).isEqualTo(Locale.ENGLISH);
    }

    @Test
    void a_malformed_header_falls_back_to_english() {
        requestAsking("fr;q=banana");

        assertThat(MailLanguage.resolve(null)).isEqualTo(Locale.ENGLISH);
    }

    @Test
    void no_header_and_no_request_fall_back_to_english() {
        requestAsking(null);
        assertThat(MailLanguage.resolve(null)).isEqualTo(Locale.ENGLISH);

        RequestContextHolder.resetRequestAttributes();
        assertThat(MailLanguage.resolve(null)).isEqualTo(Locale.ENGLISH);
    }

    @Test
    void the_language_a_request_asks_for_is_known_when_nido_speaks_it() {
        requestAsking("fr-FR,fr;q=0.9,en;q=0.8");
        assertThat(MailLanguage.requested()).contains(Language.FR);

        requestAsking("de-DE,de;q=0.9");
        assertThat(MailLanguage.requested()).isEmpty();

        RequestContextHolder.resetRequestAttributes();
        assertThat(MailLanguage.requested()).isEmpty();
    }
}
