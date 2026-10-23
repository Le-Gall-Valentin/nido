package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.model.CalendarException;
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
        LocalDateTime startLocal = IcalSupport.toLocal(start, zone);
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
        Optional<DtEnd<Temporal>> dtEnd = event.getProperty(Property.DTEND);
        LocalDateTime end;
        if (dtEnd.isPresent() && dtEnd.get().getDate() != null) {
            end = IcalSupport.toLocal(dtEnd.get().getDate(), zone);
        } else {
            Optional<net.fortuna.ical4j.model.property.Duration> duration = event.getProperty(Property.DURATION);
            if (duration.isPresent()) {
                try {
                    end = startLocal.plus(duration.get().getDuration());
                } catch (DateTimeException | ArithmeticException incomputable) {
                    return tooLate(startLocal);
                }
            } else {
                end = allDay ? startLocal.plusDays(1) : startLocal;
            }
        }
        return end.getYear() > MAX_YEAR ? tooLate(startLocal) : end;
    }

    private static LocalDateTime tooLate(LocalDateTime startLocal) {
        return startLocal.plusDays(EventScheduleValidator.MAX_EVENT_DAYS + 1L);
    }

    /** Whether nido can hold it as it is: no event covers more than a calendar read shows. */
    boolean fits() {
        try {
            EventScheduleValidator.validateEvent(allDay, startDate(), startTime(), endDate(), endTime());
            return true;
        } catch (CalendarException refused) {
            return false;
        }
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
