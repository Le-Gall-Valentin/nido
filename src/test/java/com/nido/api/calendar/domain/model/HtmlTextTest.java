package com.nido.api.calendar.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlTextTest {

    @Test
    void htmlBecomesTheTextItShows() {
        assertThat(HtmlText.toPlain("<b>Apporter</b> les maillots<br>Rendez-vous au "
            + "<a href=\"https://club.example/plan\">gymnase</a> &amp; parking<p>Merci&nbsp;!</p>"))
            .isEqualTo("Apporter les maillots\nRendez-vous au gymnase (https://club.example/plan) & parking\nMerci !");
    }

    @Test
    void aLinkShowingItsOwnAddressIsNotRepeated() {
        assertThat(HtmlText.toPlain("<a href=\"https://x.example\">https://x.example</a>")).isEqualTo("https://x.example");
    }

    @Test
    void numericEntitiesAreDecoded() {
        assertThat(HtmlText.toPlain("caf&#233; &#x2014; <i>ok</i>")).isEqualTo("café — ok");
    }

    @Test
    void plainTextIsNotTakenForHtml() {
        assertThat(HtmlText.looksLikeHtml("3 < 5 et 6 > 4")).isFalse();
        assertThat(HtmlText.looksLikeHtml("Ligne<br>suivante")).isTrue();
    }
}
