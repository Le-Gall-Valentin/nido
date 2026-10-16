package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CalendarFeed;
import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.model.EventRecurrenceProjector;
import com.nido.api.calendar.domain.model.FeedSeries;
import com.nido.api.calendar.domain.model.RecurrenceRules;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.port.out.IcalWriter;
import com.nido.api.infrastructure.NidoVersion;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.TzId;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.Color;
import net.fortuna.ical4j.model.property.Created;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.ExDate;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Method;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.RecurrenceId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.XProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Writes a calendar with ical4j. The space's events are written in full — a series as its RRULE, its cancellations as
 * EXDATE, its occurrences edited on their own as events of the same UID with a RECURRENCE-ID —, in the space's zone,
 * whose VTIMEZONE comes from the definitions ical4j ships (never fetched: see ical4j.properties). The other sources are
 * single events, their title saying what they are.
 */
@Component
public class Ical4jCalendarWriterAdapter implements IcalWriter {

    /** RFC 7986 COLOR is a CSS colour name: the nearest one to each colour an event may have. */
    private static final Map<String, String> CSS_COLORS = Map.of(
        "event-violet", "blueviolet", "event-magenta", "mediumvioletred", "event-cyan", "darkcyan",
        "event-graphite", "dimgray", "event-indigo", "indigo", "event-brique", "firebrick");

    private static final Map<CalendarSourceType, String> PREFIX_FR = Map.of(
        CalendarSourceType.TASK, "Tâche", CalendarSourceType.MEAL, "Repas",
        CalendarSourceType.FINANCE, "Finance", CalendarSourceType.SAVINGS, "Épargne");
    private static final Map<CalendarSourceType, String> PREFIX_EN = Map.of(
        CalendarSourceType.TASK, "Task", CalendarSourceType.MEAL, "Meal",
        CalendarSourceType.FINANCE, "Finance", CalendarSourceType.SAVINGS, "Savings");

    @Override
    public String write(CalendarFeed feed, Locale locale) {
        ZoneId zone = feed.zone();
        Calendar calendar = new Calendar()
            .withProdId("-//nido//nido " + NidoVersion.current() + "//EN")
            .withDefaults()
            .withProperty(new XProperty("X-WR-CALNAME", feed.name()))
            .withProperty(new XProperty("X-WR-TIMEZONE", zone.getId()))
            .withProperty(new XProperty("REFRESH-INTERVAL", new ParameterList(List.of(new Value("DURATION"))), "PT1H"))
            .withProperty(new XProperty("X-PUBLISHED-TTL", "PT1H"))
            .withProperty(new Method("PUBLISH"))
            .getFluentTarget();
        TimeZone timeZone = IcalSupport.registry().getTimeZone(zone.getId());
        if (timeZone != null) {
            calendar.add(timeZone.getVTimeZone());
        }

        for (CalendarEvent event : feed.events()) {
            calendar.add(eventOf(event, event.id() + "@nido", zone));
        }
        for (FeedSeries series : feed.series()) {
            writeSeries(calendar, series, zone);
        }
        Map<CalendarSourceType, String> prefixes = "fr".equals(locale.getLanguage()) ? PREFIX_FR : PREFIX_EN;
        for (CalendarOccurrence other : feed.others()) {
            VEvent event = vevent(other.allDay(), other.startDate(), other.startTime(), other.endDate(), other.endTime(),
                prefixes.getOrDefault(other.source(), other.source().name()) + " · " + other.title(), zone);
            event.add(new Uid(other.source().name() + "-" + other.sourceId() + "@nido"));
            calendar.add(event);
        }

        StringWriter out = new StringWriter();
        try {
            new CalendarOutputter(false).output(calendar, out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toString();
    }

    private static void writeSeries(Calendar calendar, FeedSeries feedSeries, ZoneId zone) {
        RecurringEventSeries series = feedSeries.series();
        Optional<LocalDate> first = EventRecurrenceProjector.firstOccurrence(series);
        if (first.isEmpty()) {
            return; // a series with no occurrence left has nothing to write
        }
        String uid = series.id() + "@nido";
        LocalDate start = first.get();
        VEvent master = vevent(series.allDay(), start, series.startTime(), start.plusDays(series.durationDays()),
            series.endTime(), series.title(), zone);
        master.add(new Uid(uid));
        master.add(new Created(series.createdAt()));
        describe(master, series.description(), series.location(), series.color());
        master.add(new RRule<>(new Recur<Temporal>(RecurrenceRules.toRrule(series, zone))));
        List<LocalDate> cancelled = feedSeries.cancelledSlots().stream().filter(slot -> !slot.isBefore(start)).sorted().toList();
        if (!cancelled.isEmpty()) {
            master.add(series.allDay()
                ? new ExDate<>(new ParameterList(List.of(Value.DATE)), new DateList<>(cancelled))
                : new ExDate<>(new ParameterList(List.of(new TzId(zone.getId()))),
                    new DateList<>(cancelled.stream().map(slot -> slot.atTime(series.startTime()).atZone(zone)).toList())));
        }
        calendar.add(master);
        for (CalendarEvent edited : feedSeries.editedOccurrences()) {
            VEvent occurrence = eventOf(edited, uid, zone);
            LocalDate slot = edited.recurringOriginalDate();
            occurrence.add(series.allDay()
                ? new RecurrenceId<>(slot)
                : new RecurrenceId<>(new ParameterList(List.of(new TzId(zone.getId()))), slot.atTime(series.startTime()).atZone(zone)));
            calendar.add(occurrence);
        }
    }

    private static VEvent eventOf(CalendarEvent event, String uid, ZoneId zone) {
        VEvent vevent = vevent(event.allDay(), event.startDate(), event.startTime(), event.endDate(), event.endTime(),
            event.title(), zone);
        vevent.add(new Uid(uid));
        vevent.add(new Created(event.createdAt()));
        describe(vevent, event.description(), event.location(), event.color());
        return vevent;
    }

    /** An all-day event ends the day after its last day: iCalendar's end is exclusive. */
    private static VEvent vevent(boolean allDay, LocalDate startDate, LocalTime startTime, LocalDate endDate,
                                 LocalTime endTime, String summary, ZoneId zone) {
        return allDay
            ? new VEvent(startDate, endDate.plusDays(1), summary)
            : new VEvent(startDate.atTime(startTime).atZone(zone), endDate.atTime(endTime).atZone(zone), summary);
    }

    private static void describe(VEvent vevent, String description, String location, String color) {
        if (description != null && !description.isBlank()) {
            vevent.add(new Description(description));
        }
        if (location != null && !location.isBlank()) {
            vevent.add(new Location(location));
        }
        if (color != null && CSS_COLORS.containsKey(color)) {
            vevent.add(new Color(null, CSS_COLORS.get(color)));
        }
    }
}
