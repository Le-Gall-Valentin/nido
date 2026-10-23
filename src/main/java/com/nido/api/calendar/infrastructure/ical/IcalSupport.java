package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.model.CalendarException;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarParserFactory;
import net.fortuna.ical4j.data.ContentHandlerContext;
import net.fortuna.ical4j.data.DefaultPropertyFactorySupplier;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentFactory;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyFactory;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.property.XProperty;
import net.fortuna.ical4j.util.CompatibilityHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Everything the calendar asks of ical4j beyond writing: reading a document tolerantly, and placing what it reads in a
 * space's zone. The hints are also in ical4j.properties; set here too, so no reading depends on the file being found.
 */
public final class IcalSupport {

    private static final Logger log = LoggerFactory.getLogger(IcalSupport.class);

    /** Only for writing (ical4j's bundled VTIMEZONE text): no document is ever read with this shared registry. */
    private static final TimeZoneRegistry REGISTRY;

    /**
     * What ical4j builds a VTIMEZONE's rules from (TZID, TZOFFSETFROM, TZOFFSETTO), typed: one that does not parse
     * refuses the whole file, for no time of a document written in that zone could be placed.
     */
    private static final Set<String> ZONE_TYPED = Set.of(Property.TZID, Property.TZOFFSETFROM, Property.TZOFFSETTO);

    /**
     * The properties an event is read by, typed: when it is and how it repeats (DTSTART, DTEND, DURATION, RRULE, RDATE,
     * EXDATE, EXRULE, RECURRENCE-ID — nido's reads and ical4j's expansion), and what nido shows or groups by (UID,
     * SUMMARY, DESCRIPTION, LOCATION, STATUS). One of these that does not parse is never guessed: it is replaced by
     * {@link #UNREADABLE}, which sets its whole event aside (see IcalGroups) and leaves the rest of the file read.
     *
     * <p>Any other property — CREATED, LAST-MODIFIED, DTSTAMP, GEO, ORGANIZER, DUE, SEQUENCE (read as text, with its own
     * fallback: see IcalGroups) — is kept as written, untyped, when it does not parse: ical4j would otherwise refuse the
     * whole file for a value nido never reads, and Google writes CREATED:00001231T000000Z on some events.
     */
    private static final Set<String> EVENT_TYPED = Set.of(
        Property.DTSTART, Property.DTEND, Property.DURATION, Property.RRULE, Property.RDATE, Property.EXDATE,
        Property.EXRULE, Property.RECURRENCE_ID, Property.UID, Property.SUMMARY, Property.DESCRIPTION, Property.LOCATION,
        Property.STATUS);

    /** What stands in for a property nido reads whose value does not parse; its value is the property's name. */
    static final String UNREADABLE = "X-NIDO-UNREADABLE";

    /** The names RFC 5545 gives properties, as ical4j lists them: to name a factory that makes no blank instance. */
    private static final List<String> PROPERTY_NAMES = Arrays.stream(Property.class.getFields())
        .filter(field -> Modifier.isStatic(field.getModifiers()) && field.getType() == String.class)
        .map(IcalSupport::valueOf).toList();

    /** ical4j's own factories, made tolerant as above. Stateless: shared. */
    private static final List<PropertyFactory<?>> FACTORIES;

    /** ical4j builds a zone's rules from the machine's clock: what nido weighs and compares of a zone is reckoned from it too. */
    private static final Clock MACHINE_CLOCK = Clock.systemUTC();

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

    /** A document read, with what its own zones hold: the changes they list, and the ids of ical4j's pool they draw. */
    record Read(Calendar calendar, long zoneChanges, int pooledZones) {}

    public static Calendar parse(String content) {
        return read(content).calendar();
    }

    static Read read(String content) {
        Instant now = MACHINE_CLOCK.instant();
        BoundedZones zones = new BoundedZones(now);
        DocumentZones registry = new DocumentZones(now);
        List<ComponentFactory<?>> components = zones.factories();
        try {
            Calendar calendar = new CalendarBuilder(CalendarParserFactory.getInstance().get(), new ContentHandlerContext()
                .withPropertyFactorySupplier(() -> FACTORIES).withComponentFactorySupplier(() -> components),
                registry).build(new StringReader(content));
            return new Read(calendar, zones.spent(), registry.pooled());
        } catch (ParserException | IOException | RuntimeException e) {
            throw new CalendarException.NotAnIcalFile();
        } catch (StackOverflowError | OutOfMemoryError exhausted) {
            // What reading one document exhausts, past every bound nido sets, costs that document and not the thread
            // reading it: a followed calendar is then remembered as unreadable, not read again at every view. Said, for
            // the memory may have run out under another thread's load rather than this document's.
            log.warn("A calendar document was refused: reading it ran into {}", exhausted.getClass().getSimpleName());
            throw new CalendarException.NotAnIcalFile();
        }
    }

    private static PropertyFactory<?> tolerant(PropertyFactory<?> factory) {
        if (ZONE_TYPED.stream().anyMatch(factory::supports)) {
            return factory;
        }
        Optional<String> name = nameOf(factory);
        if (name.isEmpty()) {
            return factory; // its name cannot be learnt: it stays as ical4j made it
        }
        return EVENT_TYPED.contains(name.get()) ? new Marked(factory, name.get()) : new Tolerant(factory, name.get());
    }

    /** The name of the properties a factory makes: off a blank one, or, when it makes none (DUE), off RFC 5545's names. */
    private static Optional<String> nameOf(PropertyFactory<?> factory) {
        try {
            return Optional.of(factory.createProperty().getName());
        } catch (RuntimeException noBlankInstance) {
            return PROPERTY_NAMES.stream().filter(factory::supports).findFirst();
        }
    }

    private static String valueOf(Field constant) {
        try {
            return (String) constant.get(null);
        } catch (IllegalAccessException impossible) {
            throw new IllegalStateException(impossible);
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

    /** A property nido reads: typed when its value parses, {@link #UNREADABLE} otherwise — never kept untyped under its name. */
    private record Marked(PropertyFactory<?> typed, String name) implements PropertyFactory<Property> {

        @Override
        public Property createProperty() {
            return typed.createProperty();
        }

        @Override
        public Property createProperty(ParameterList parameters, String value) {
            try {
                return typed.createProperty(parameters, value);
            } catch (RuntimeException unreadable) {
                return new XProperty(UNREADABLE, name);
            }
        }

        @Override
        public boolean supports(String candidate) {
            return typed.supports(candidate);
        }
    }

    /** Whether a component holds a property nido reads that did not parse. */
    static boolean unreadable(Component component) {
        return component.getProperty(UNREADABLE).isPresent();
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
