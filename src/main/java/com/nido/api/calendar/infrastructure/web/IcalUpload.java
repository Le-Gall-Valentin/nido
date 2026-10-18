package com.nido.api.calendar.infrastructure.web;

import com.nido.api.calendar.domain.model.CalendarException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** An uploaded iCalendar file, read no further than an import takes: one byte past the limit refuses it. */
final class IcalUpload {

    static final int MAX_MEGABYTES = 5;
    private static final int MAX_BYTES = MAX_MEGABYTES * 1024 * 1024;
    private static final String BOM = "\uFEFF";

    private IcalUpload() {}

    /** An IOException is a client gone mid-upload: it reaches Spring as it is, which knows it for what it is. */
    static String read(InputStream body) throws IOException {
        byte[] bytes = body.readNBytes(MAX_BYTES + 1);
        if (bytes.length > MAX_BYTES) {
            throw new CalendarException.ImportFileTooLarge(MAX_MEGABYTES);
        }
        String text = new String(bytes, StandardCharsets.UTF_8);
        // A byte order mark some applications write first: ical4j reads past it, but the import does not rely on that.
        return text.startsWith(BOM) ? text.substring(1) : text;
    }
}
