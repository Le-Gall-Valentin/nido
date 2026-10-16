package com.nido.api.calendar.infrastructure.ical;

import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Set;

/** Reads back what nido writes, the way a calendar application would: ical4j expanding the rule. */
final class IcalTestSupport {

    private IcalTestSupport() {}

    static List<VEvent> events(String ics) {
        return IcalSupport.parse(ics).getComponents(Component.VEVENT);
    }

    static VEvent master(String ics, String uid) {
        return events(ics).stream()
            .filter(e -> e.getProperty(Property.UID).map(Property::getValue).orElse("").equals(uid))
            .filter(e -> e.getProperty(Property.RECURRENCE_ID).isEmpty())
            .findFirst().orElseThrow();
    }

    /** The local start of every occurrence ical4j expands between two days, in {@code zone}. */
    static List<LocalDateTime> starts(VEvent event, ZoneId zone, LocalDate from, LocalDate to) {
        Set<Period<Temporal>> set = event.calculateRecurrenceSet(new Period<>(from.atStartOfDay(zone), to.plusDays(1).atStartOfDay(zone)));
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        // ical4j's period end is inclusive: an all-day occurrence starting exactly at it is outside the days asked for.
        return set.stream().map(p -> IcalSupport.toLocal(p.getStart(), zone))
            .filter(start -> start.isBefore(end)).sorted().toList();
    }

    static ZonedDateTime at(LocalDate day, ZoneId zone) {
        return day.atStartOfDay(zone);
    }
}
