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

    static IcalTexts of(Component component, Locale locale) {
        List<String> cut = new ArrayList<>();
        String title = read(component, Property.SUMMARY);
        if (title == null) {
            title = "fr".equals(locale.getLanguage()) ? "(Sans titre)" : "(Untitled)";
        }
        String description = read(component, Property.DESCRIPTION);
        if (HtmlText.looksLikeHtml(description)) {
            description = HtmlText.toPlain(description);
        }
        return new IcalTexts(fit(title, TITLE, "title", cut), fit(description, DESCRIPTION, "description", cut),
            fit(read(component, Property.LOCATION), LOCATION, "location", cut), List.copyOf(cut));
    }

    /** A component's title, or null: what an ignored element is named by in the report. */
    static String titleOf(Component component) {
        return read(component, Property.SUMMARY);
    }

    private static String read(Component component, String name) {
        return component.getProperty(name).map(Property::getValue).map(String::strip).filter(s -> !s.isEmpty()).orElse(null);
    }

    private static String fit(String text, int length, String what, List<String> cut) {
        if (text == null || text.length() <= length) {
            return text;
        }
        cut.add(what);
        return text.substring(0, length);
    }
}
