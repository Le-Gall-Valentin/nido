package com.nido.api.mail.infrastructure.render;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.stream.Collectors;

/**
 * The plain-text part of a mail, taken from its HTML: every element the kit marks
 * {@code data-text-line}, in order, one paragraph each. A link gives its label and then its address;
 * a link that shows its own address gives it once. Nothing unmarked is kept — layout tables, the
 * hidden preheader — so the text reads as the mail, not as its scaffolding.
 */
public class HtmlToTextConverter {

    public String convert(Document document) {
        return document.select("[data-text-line]").stream()
            .map(HtmlToTextConverter::line)
            .filter(line -> !line.isBlank())
            .collect(Collectors.joining("\n\n", "", "\n"));
    }

    private static String line(Element element) {
        String text = element.text().strip();
        if (!element.is("a[href]")) {
            return text;
        }
        String href = element.attr("href");
        return text.isEmpty() || text.equals(href) ? href : text + "\n" + href;
    }
}
