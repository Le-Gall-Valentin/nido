package com.nido.api.mail.infrastructure.render;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlToTextConverterTest {

    private final HtmlToTextConverter converter = new HtmlToTextConverter();

    @Test
    void writes_each_marked_element_as_its_own_paragraph_and_ignores_the_rest() {
        String html = """
            <html><body>
              <div data-text-skip>Preheader nobody reads</div>
              <table><tr><td>
                <h1 data-text-line>Title</h1>
                <p data-text-line>First   paragraph
                   on two lines.</p>
                <p>Not marked, so not in the text.</p>
              </td></tr></table>
            </body></html>""";

        assertThat(converter.convert(Jsoup.parse(html)))
            .isEqualTo("Title\n\nFirst paragraph on two lines.\n");
    }

    @Test
    void a_labelled_link_gives_its_label_then_its_address() {
        String html = "<body><a data-text-line href=\"https://nido.example/reset\">Choose a password</a></body>";

        assertThat(converter.convert(Jsoup.parse(html)))
            .isEqualTo("Choose a password\nhttps://nido.example/reset\n");
    }

    @Test
    void a_link_that_shows_its_own_address_gives_it_once() {
        String html = "<body><a data-text-line href=\"https://nido.example/reset\">https://nido.example/reset</a></body>";

        assertThat(converter.convert(Jsoup.parse(html))).isEqualTo("https://nido.example/reset\n");
    }
}
