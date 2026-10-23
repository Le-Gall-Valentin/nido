package com.nido.api.calendar.infrastructure.web;

import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

/** How an iCalendar document goes out: its type, and a file name any system accepts. */
final class IcalFiles {

    static final MediaType TEXT_CALENDAR = new MediaType("text", "calendar", StandardCharsets.UTF_8);

    /** The letters no accent can be taken off, lower case: what each is written as in a file name. */
    private static final Map<Integer, String> SPELLED = Map.of(
        (int) 'ß', "ss", (int) 'æ', "ae", (int) 'œ', "oe", (int) 'ø', "o", (int) 'ł', "l", (int) 'đ', "d", (int) 'þ', "th",
        (int) 'ð', "d");

    private IcalFiles() {}

    /** "nido-chez-nous-2026-10-13.ics": accents dropped, anything else but letters and digits made a dash. */
    static String filename(String calendarName, LocalDate day) {
        String plain = Normalizer.normalize(spelledOut(calendarName == null ? "" : calendarName.toLowerCase(Locale.ROOT)),
                Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-+|-+$)", "");
        if (plain.length() > 40) {
            plain = plain.substring(0, 40).replaceAll("-+$", "");
        }
        return "nido-" + (plain.isEmpty() ? "" : plain + "-") + day + ".ics";
    }

    /** The letters no accent takes off — ß, œ, ø… —, written as they are without one, rather than dropped. */
    private static String spelledOut(String name) {
        StringBuilder out = new StringBuilder(name.length());
        name.codePoints().forEach(letter -> out.append(SPELLED.getOrDefault(letter, Character.toString(letter))));
        return out.toString();
    }
}
