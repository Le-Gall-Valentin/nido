package com.nido.api.calendar.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

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

    @Test
    void anAddressBetweenAngleBracketsIsPlainTextAndKept() {
        // How Outlook writes a Teams meeting and an organiser in plain text; and a comparison.
        String teams = "Click here to join the meeting<https://teams.microsoft.com/l/meetup-join/19%3ameeting>";
        for (String plain : new String[] {teams, "Jeanne Martin <jeanne.martin@example.com>", "x<y et y>z",
            "<mailto:jeanne@example.com>"}) {
            assertThat(HtmlText.looksLikeHtml(plain)).as(plain).isFalse();
        }
        // In HTML too: what is no tag stays.
        assertThat(HtmlText.toPlain("<p>Rejoindre<https://teams.microsoft.com/l/x></p>"))
            .isEqualTo("Rejoindre<https://teams.microsoft.com/l/x>");
    }

    @Test
    void outlooksOwnTagsAndDeclarationsAreTags() {
        assertThat(HtmlText.looksLikeHtml("<o:p></o:p>")).isTrue();
        assertThat(HtmlText.toPlain("<!DOCTYPE html><p class=MsoNormal>Bonjour<o:p></o:p></p>")).isEqualTo("Bonjour");
    }

    @Test
    void namedEntitiesOfFrenchAndTypographyAreDecoded() {
        assertThat(HtmlText.toPlain("<p>R&eacute;union &agrave; l&rsquo;&eacute;cole&hellip; 5&nbsp;&euro; &laquo;&nbsp;&OElig;uvre&nbsp;&raquo;</p>"))
            .isEqualTo("Réunion à l’école… 5 € « Œuvre »");
        assertThat(HtmlText.toPlain("<p>&Eacute;t&eacute; &AMP; hiver</p>")).isEqualTo("Été & hiver");
        assertThat(HtmlText.toPlain("<p>&unknown; reste</p>")).isEqualTo("&unknown; reste");
    }

    @Test
    void aCarriageReturnWrittenAsAnEntityIsDropped() {
        assertThat(HtmlText.toPlain("<p>Ligne&#13;\nsuivante</p>")).isEqualTo("Ligne\nsuivante");
    }

    private static void linear(String input) {
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            HtmlText.looksLikeHtml(input);
            HtmlText.toPlain(input);
        });
    }

    @Test
    void anUnclosedTagRepeatedIsReadInLinearTime() {
        linear("<a".repeat(100_000));
    }

    @Test
    void aLinkNeverClosedIsReadInLinearTime() {
        linear("<a href=\"x\">".repeat(20_000));
    }

    @Test
    void aParagraphTagNeverClosedIsReadInLinearTime() {
        linear("<p ".repeat(70_000));
    }

    @Test
    void anOpeningTagWithManyHrefsAndNoEndIsReadInLinearTime() {
        linear("<a " + "href=\"x\" ".repeat(20_000));
        linear("<a " + "href='x ".repeat(25_000));
    }

    @Test
    void longNamesAndAddressesBetweenAngleBracketsAreReadInLinearTime() {
        linear("<" + "a".repeat(200_000));
        linear("<o:" + "p".repeat(200_000));
        linear("<https://x".repeat(20_000));
    }

    @Test
    void aLongRunOfBlanksIsReadInLinearTime() {
        linear(" ".repeat(200_000) + "x");
    }

    @Test
    void aScriptNeverClosedIsReadInLinearTime() {
        linear("<script>".repeat(25_000));
        linear("<!--".repeat(50_000));
    }

    @Test
    void numericEntitiesThatAreNotTextAreKeptAsWritten() {
        for (String entity : new String[] {"&#99999999999;", "&#1114112;", "&#xFFFFFFFF;", "&#0;", "&#xD800;", "&#x1;"}) {
            assertThat(HtmlText.toPlain("a" + entity + "b")).isEqualTo("a" + entity + "b");
        }
        assertThat(HtmlText.toPlain("&#128512;")).isEqualTo("\uD83D\uDE00");
    }

    @Test
    void scriptsStylesAndCommentsAreDroppedWithTheirContent() {
        assertThat(HtmlText.toPlain("a<script>alert(1)</script>b<style>p {}</style>c<!-- x > y -->d")).isEqualTo("abcd");
        assertThat(HtmlText.toPlain("avant<script>alert(1)")).isEqualTo("avant");
    }

    @Test
    void aLinkMayUseSingleQuotes() {
        assertThat(HtmlText.toPlain("<a href='https://x.example/p'>plan</a>")).isEqualTo("plan (https://x.example/p)");
    }

    @Test
    void aLinkKeepsAQuoteOfTheOtherKindInItsAddress() {
        assertThat(HtmlText.toPlain("<a href=\"https://x.example/l'atelier\">plan</a>")).isEqualTo("plan (https://x.example/l'atelier)");
        assertThat(HtmlText.toPlain("<a href='https://x.example/\"q\"'>plan</a>")).isEqualTo("plan (https://x.example/\"q\")");
    }

    @Test
    void aLinkIsReadWhateverItsCaseAndItsOtherAttributes() {
        assertThat(HtmlText.toPlain("<A HREF=\"https://x.example/p\">plan</A>")).isEqualTo("plan (https://x.example/p)");
        assertThat(HtmlText.toPlain("<a target=\"_blank\" href = \"https://x.example/p\" rel=\"noopener\">plan</a>"))
            .isEqualTo("plan (https://x.example/p)");
    }

    @Test
    void aLinkWithoutAnAddressIsOnlyItsLabel() {
        assertThat(HtmlText.toPlain("<a name=\"haut\">ancre</a> suite")).isEqualTo("ancre suite");
    }

    @Test
    void quotesOfBothKindsNeverClosedAreReadInLinearTime() {
        linear("<a " + "href=\"x' ".repeat(20_000));
        linear("<a " + "href='x\" ".repeat(20_000));
    }
}
