package com.nido.api.calendar.infrastructure.web;

import com.nido.api.calendar.domain.model.CalendarException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** An uploaded iCalendar file, read no further than an import takes: one byte past the limit refuses it. */
final class IcalUpload {

    static final int MAX_MEGABYTES = 5;
    private static final int MAX_BYTES = MAX_MEGABYTES * 1024 * 1024;

    private IcalUpload() {}

    static String read(InputStream body) {
        try {
            byte[] bytes = body.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) {
                throw new CalendarException.ImportFileTooLarge(MAX_MEGABYTES);
            }
            String text = new String(bytes, StandardCharsets.UTF_8);
            return text.startsWith("﻿") ? text.substring(1) : text;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
