package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.model.CalendarException;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarParserFactory;
import net.fortuna.ical4j.data.ContentHandlerContext;
import net.fortuna.ical4j.data.DefaultPropertyFactorySupplier;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyFactory;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.property.XProperty;
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
import java.util.List;
import java.util.Set;

/**
 * Everything the calendar asks of ical4j beyond writing: reading a document tolerantly, and placing what it reads in a
 * space's zone. The hints are also in ical4j.properties; set here too, so no reading depends on the file being found.
 */
public final class IcalSupport {

    /** Only for writing (ical4j's bundled VTIMEZONE text): no document is ever read with this shared registry. */
    private static final TimeZoneRegistry REGISTRY;

    /**
     * The properties a reading relies on, typed: when an event is and how it repeats (DTSTART, DTEND, DURATION, RRULE,
     * RDATE, EXDATE, EXRULE, RECURRENCE-ID — nido's reads and ical4j's expansion), what nido shows or groups by (UID,
     * SUMMARY, DESCRIPTION, LOCATION, STATUS), and what ical4j builds a VTIMEZONE's rules from (TZID, TZOFFSETFROM,
     * TZOFFSETTO). One of these that does not parse still refuses the whole file: what it says is never guessed.
     *
     * <p>Any other property — CREATED, LAST-MODIFIED, DTSTAMP, GEO, ORGANIZER, SEQUENCE (read as text, with its own
     * fallback: see IcalGroups) — is kept as written, untyped, when it does not parse: ical4j would otherwise refuse the
     * whole file for a value nido never reads, and Google writes CREATED:00001231T000000Z on some events.
     */
    private static final Set<String> READ_TYPED = Set.of(
        Property.DTSTART, Property.DTEND, Property.DURATION, Property.RRULE, Property.RDATE, Property.EXDATE,
        Property.EXRULE, Property.RECURRENCE_ID, Property.UID, Property.SUMMARY, Property.DESCRIPTION, Property.LOCATION,
        Property.STATUS, Property.TZID, Property.TZOFFSETFROM, Property.TZOFFSETTO);

    /** ical4j's own factories, those of the properties not in {@link #READ_TYPED} made tolerant. Stateless: shared. */
    private static final List<PropertyFactory<?>> FACTORIES;

    static {
        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_UNFOLDING, true);
        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING, true);
        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_VALIDATION, true);
        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_OUTLOOK_COMPATIBILITY, true);
        REGISTRY = TimeZoneRegistryFactory.getInstance().createRegistry();
        FACTORIES = new DefaultPropertyFactorySupplier().get().stream()
            .<PropertyFactory<?>>map(IcalSupport::tolerant).toList();
    }

    private IcalSupport() {}

    public static Calendar parse(String content) {
        try {
            return new CalendarBuilder(CalendarParserFactory.getInstance().get(),
                new ContentHandlerContext().withPropertyFactorySupplier(() -> FACTORIES), new DocumentZones())
                .build(new StringReader(content));
        } catch (ParserException | IOException | RuntimeException e) {
            throw new CalendarException.NotAnIcalFile();
        }
    }

    private static PropertyFactory<?> tolerant(PropertyFactory<?> factory) {
        if (READ_TYPED.stream().anyMatch(factory::supports)) {
            return factory;
        }
        try {
            return new Tolerant(factory, factory.createProperty().getName());
        } catch (RuntimeException noBlankInstance) {
            return factory; // its name cannot be learnt: it stays as ical4j made it
        }
    }

    /** A property nido does not read: typed when its value parses, kept as written otherwise. */
    private record Tolerant(PropertyFactory<?> typed, String name) implements PropertyFactory<Property> {

        @Override
        public Property createProperty() {
            return typed.createProperty();
        }

        @Override
        public Property createProperty(ParameterList parameters, String value) {
            try {
                return typed.createProperty(parameters, value);
            } catch (RuntimeException unreadable) {
                return new XProperty(name, parameters, value);
            }
        }

        @Override
        public boolean supports(String candidate) {
            return typed.supports(candidate);
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
