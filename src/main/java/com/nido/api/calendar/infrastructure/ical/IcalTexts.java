package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.model.HtmlText;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The texts of an event of a file, as nido keeps them: plain, within its lengths; {@code cut} names what was shortened. */
record IcalTexts(String title, String description, String location, List<String> cut) {

    static final int TITLE = 200;
    static final int DESCRIPTION = 2000;
    static final int LOCATION = 200;
    /**
     * What is read of a raw text before anything is done to it: four times what nido keeps, so the markup of a
     * description has room to go. A file is untrusted, and its texts are cut here, not after being worked on.
     */
    static final int RAW_TEXT_CAP = 4;

    static IcalTexts of(Component component, Locale locale) {
        List<String> cut = new ArrayList<>();
        String title = read(component, Property.SUMMARY, TITLE, "title", cut);
        if (title == null) {
            title = untitled(locale);
        }
        String description = read(component, Property.DESCRIPTION, DESCRIPTION, "description", cut);
        if (HtmlText.looksLikeHtml(description)) {
            description = HtmlText.toPlain(description);
            description = description.isEmpty() ? null : description;
        }
        return new IcalTexts(fit(title, TITLE, "title", cut), fit(description, DESCRIPTION, "description", cut),
            fit(read(component, Property.LOCATION, LOCATION, "location", cut), LOCATION, "location", cut), List.copyOf(cut));
    }

    /** A component's title, within nido's length: what an ignored element is named by in the report. Null if it has none. */
    static String titleOf(Component component) {
        List<String> cut = new ArrayList<>();
        return fit(read(component, Property.SUMMARY, TITLE, "title", cut), TITLE, "title", cut);
    }

    /** The same for an event, named when it has no title. Costs a title's length, whatever the event holds. */
    static String titleOrUntitled(Component component, Locale locale) {
        String title = titleOf(component);
        return title != null ? title : untitled(locale);
    }

    private static String untitled(Locale locale) {
        return "fr".equals(locale.getLanguage()) ? "(Sans titre)" : "(Untitled)";
    }

    /** A text, cut first to its raw cap, then freed of the control characters a database refuses; null when empty. */
    private static String read(Component component, String name, int length, String what, List<String> cut) {
        String raw = component.getProperty(name).map(Property::getValue).orElse(null);
        if (raw == null) {
            return null;
        }
        String capped = fit(raw, length * RAW_TEXT_CAP, what, cut);
        String text = withoutControls(capped).strip();
        return text.isEmpty() ? null : text;
    }

    /** Every control character but the tab and the line break: a NUL fails the insert, the rest has no place in a text. */
    private static String withoutControls(String text) {
        StringBuilder out = new StringBuilder(text.length());
        text.codePoints().filter(c -> !Character.isISOControl(c) || c == '\t' || c == '\n').forEach(out::appendCodePoint);
        return out.toString();
    }

    private static String fit(String text, int length, String what, List<String> cut) {
        if (text == null || text.length() <= length) {
            return text;
        }
        if (!cut.contains(what)) {
            cut.add(what);
        }
        // Never between the two halves of a character outside the basic plane.
        int end = Character.isHighSurrogate(text.charAt(length - 1)) ? length - 1 : length;
        return text.substring(0, end);
    }
}
