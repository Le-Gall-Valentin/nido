package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.model.EventScheduleValidator;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Optional;

/**
 * When an event of a file happens, in the space's zone. {@code foreignZone}: the zone it was written in, when another one
 * nido knows; {@code unknownZone}: a zone nobody knows, its times then taken as the space's.
 */
record IcalTimes(boolean allDay, LocalDateTime start, LocalDateTime end, String foreignZone, String unknownZone) {

    private static final int MAX_YEAR = 9999;

    static Optional<IcalTimes> of(VEvent event, ZoneId zone) {
        Optional<DtStart<Temporal>> dtStart = event.getProperty(Property.DTSTART);
        if (dtStart.isEmpty() || dtStart.get().getDate() == null) {
            return Optional.empty();
        }
        Temporal start = dtStart.get().getDate();
        boolean allDay = start instanceof LocalDate;
        LocalDateTime startLocal;
        try {
            startLocal = IcalSupport.toLocal(start, zone);
        } catch (DateTimeException | ArithmeticException | IllegalArgumentException unreadable) {
            return Optional.empty();
        }
        LocalDateTime endLocal = endOf(event, startLocal, allDay, zone);
        Optional<String> tzid = dtStart.get().getParameter(Parameter.TZID).map(Parameter::getValue);
        // Another name of the same rules (Asia/Calcutta for Asia/Kolkata) changes no time: nothing was converted.
        String foreign = tzid.filter(id -> start instanceof ZonedDateTime zoned
            && !zoned.getZone().getRules().equals(zone.getRules())).orElse(null);
        String unknown = tzid.filter(id -> start instanceof LocalDateTime).orElse(null);
        return Optional.of(new IcalTimes(allDay, startLocal, endLocal.isBefore(startLocal) ? startLocal : endLocal,
            foreign, unknown));
    }

    /**
     * Where it ends. An end nido cannot compute (a duration of a billion days, a date past year 9999) is one no event
     * of nido can reach: it is answered with a day more than an event may last, which the caller sets aside as too long.
     */
    private static LocalDateTime endOf(VEvent event, LocalDateTime startLocal, boolean allDay, ZoneId zone) {
        try {
            LocalDateTime end = computeEnd(event, startLocal, allDay, zone);
            return end.getYear() > MAX_YEAR ? tooLate(startLocal) : end;
        } catch (DateTimeException | ArithmeticException | IllegalArgumentException incomputable) {
            return tooLate(startLocal);
        }
    }

    private static LocalDateTime tooLate(LocalDateTime startLocal) {
        return startLocal.plusDays(EventScheduleValidator.MAX_EVENT_DAYS + 1L);
    }

    private static LocalDateTime computeEnd(VEvent event, LocalDateTime startLocal, boolean allDay, ZoneId zone) {
        Optional<DtEnd<Temporal>> dtEnd = event.getProperty(Property.DTEND);
        if (dtEnd.isPresent() && dtEnd.get().getDate() != null) {
            return IcalSupport.toLocal(dtEnd.get().getDate(), zone);
        }
        Optional<net.fortuna.ical4j.model.property.Duration> duration = event.getProperty(Property.DURATION);
        if (duration.isPresent()) {
            return startLocal.plus(duration.get().getDuration());
        }
        return allDay ? startLocal.plusDays(1) : startLocal;
    }

    LocalDate startDate() {
        return start.toLocalDate();
    }

    /** The last day it covers: the end of an all-day event is exclusive, the day after. */
    LocalDate endDate() {
        if (!allDay) {
            return end.toLocalDate();
        }
        return end.toLocalDate().isAfter(start.toLocalDate()) ? end.toLocalDate().minusDays(1) : start.toLocalDate();
    }

    LocalTime startTime() {
        return allDay ? null : start.toLocalTime();
    }

    LocalTime endTime() {
        return allDay ? null : end.toLocalTime();
    }

    /** The same length, starting at {@code other}: one of its occurrences. */
    IcalTimes at(LocalDateTime other) {
        return new IcalTimes(allDay, other, other.plus(Duration.between(start, end)), foreignZone, unknownZone);
    }
}
