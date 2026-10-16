package com.nido.api.calendar.infrastructure.web;

import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Locale;

/** How an iCalendar document goes out: its type, and a file name any system accepts. */
final class IcalFiles {

    static final MediaType TEXT_CALENDAR = new MediaType("text", "calendar", StandardCharsets.UTF_8);

    private IcalFiles() {}

    /** "nido-chez-nous-2026-10-13.ics": accents dropped, anything else but letters and digits made a dash. */
    static String filename(String calendarName, LocalDate day) {
        String plain = Normalizer.normalize(calendarName == null ? "" : calendarName, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-+|-+$)", "");
        if (plain.length() > 40) {
            plain = plain.substring(0, 40).replaceAll("-+$", "");
        }
        return "nido-" + (plain.isEmpty() ? "" : plain + "-") + day + ".ics";
    }
}
