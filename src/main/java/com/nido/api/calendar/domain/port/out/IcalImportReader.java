package com.nido.api.calendar.domain.port.out;

import com.nido.api.calendar.domain.model.ImportReading;
import com.nido.api.calendar.domain.model.ImportTarget;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

public interface IcalImportReader {
    /**
     * Reads an iCalendar file into what a space whose zone is {@code zone} can hold; {@code today} bounds the occurrences
     * of a rule unfolded, {@code locale} names an untitled event. Skips the UIDs {@code target} holds already, and refuses
     * the file ({@code ImportTooLarge}) as soon as what it would write passes {@code target}'s rows. Writes nothing.
     */
    ImportReading read(String content, ZoneId zone, LocalDate today, Locale locale, ImportTarget target);
}
