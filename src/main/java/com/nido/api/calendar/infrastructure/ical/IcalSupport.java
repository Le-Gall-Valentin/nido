package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.model.CalendarException;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.util.CompatibilityHints;

import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;

/**
 * Everything the calendar asks of ical4j beyond writing: reading a document tolerantly, and placing what it reads in a
 * space's zone. The hints are also in ical4j.properties; set here too, so no reading depends on the file being found.
 */
public final class IcalSupport {

    private static final TimeZoneRegistry REGISTRY;

    static {
        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_UNFOLDING, true);
        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING, true);
        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_VALIDATION, true);
        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_OUTLOOK_COMPATIBILITY, true);
        REGISTRY = TimeZoneRegistryFactory.getInstance().createRegistry();
    }

    private IcalSupport() {}

    public static Calendar parse(String content) {
        try {
            return new CalendarBuilder(REGISTRY).build(new StringReader(content));
        } catch (ParserException | IOException | RuntimeException e) {
            throw new CalendarException.NotAnIcalFile();
        }
    }

    public static TimeZoneRegistry registry() {
        return REGISTRY;
    }

    /**
     * Where a date-time of a document falls in {@code zone}. A zoned or UTC time is converted; a "floating" one (no zone
     * at all, or a zone ical4j does not know) is taken as it is, in the space's zone. A date is the start of its day.
     */
    public static LocalDateTime toLocal(Temporal temporal, ZoneId zone) {
        return switch (temporal) {
            case ZonedDateTime zoned -> zoned.withZoneSameInstant(zone).toLocalDateTime();
            case OffsetDateTime offset -> offset.atZoneSameInstant(zone).toLocalDateTime();
            case Instant instant -> instant.atZone(zone).toLocalDateTime();
            case LocalDateTime local -> local;
            case LocalDate date -> date.atStartOfDay();
            default -> throw new IllegalArgumentException("Unexpected temporal " + temporal.getClass());
        };
    }
}
