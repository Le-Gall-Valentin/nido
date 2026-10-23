package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.model.CalendarFeed;
import com.nido.api.calendar.domain.model.DayRule;
import com.nido.api.calendar.domain.model.EventRecurrenceProjector;
import com.nido.api.calendar.domain.model.FeedSeries;
import com.nido.api.calendar.domain.model.RecurrenceInterval;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What nido exports, a calendar application reads back as the same occurrences: for every rule nido has, ical4j expands
 * the written RRULE into exactly the days the projector gives, over two years — in a zone with summer time.
 */
class RoundTripTest {

    private static final ZoneId TORONTO = ZoneId.of("America/Toronto");
    private final Ical4jCalendarWriterAdapter writer = new Ical4jCalendarWriterAdapter();

    static Stream<Arguments> rules() {
        List<Arguments> rules = new ArrayList<>();
        for (int count = 1; count <= 3; count++) {
            for (boolean allDay : List.of(true, false)) {
                for (LocalDate anchor : List.of(LocalDate.of(2026, 1, 31), LocalDate.of(2028, 2, 29))) {
                    rules.add(Arguments.of(RecurrenceInterval.DAILY, count, null, null, anchor, allDay));
                }
                rules.add(Arguments.of(RecurrenceInterval.WEEKLY, count, Set.of(DayOfWeek.MONDAY), null, LocalDate.of(2026, 12, 28), allDay));
                rules.add(Arguments.of(RecurrenceInterval.WEEKLY, count, Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.SUNDAY),
                    null, LocalDate.of(2026, 12, 31), allDay));
                for (LocalDate anchor : List.of(LocalDate.of(2026, 1, 14), LocalDate.of(2026, 1, 29), LocalDate.of(2026, 1, 30),
                    LocalDate.of(2026, 1, 31), LocalDate.of(2028, 2, 29))) {
                    rules.add(Arguments.of(RecurrenceInterval.MONTHLY, count, null, DayRule.SAME_DAY, anchor, allDay));
                    rules.add(Arguments.of(RecurrenceInterval.YEARLY, count, null, DayRule.SAME_DAY, anchor, allDay));
                }
                for (LocalDate anchor : List.of(LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 28))) {
                    rules.add(Arguments.of(RecurrenceInterval.MONTHLY, count, null, DayRule.WEEKDAY_OF_MONTH, anchor, allDay));
                    rules.add(Arguments.of(RecurrenceInterval.YEARLY, count, null, DayRule.WEEKDAY_OF_MONTH, anchor, allDay));
                }
                for (LocalDate anchor : List.of(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 10, 25), LocalDate.of(2026, 5, 31))) {
                    rules.add(Arguments.of(RecurrenceInterval.MONTHLY, count, null, DayRule.LAST_WEEKDAY_OF_MONTH, anchor, allDay));
                    rules.add(Arguments.of(RecurrenceInterval.YEARLY, count, null, DayRule.LAST_WEEKDAY_OF_MONTH, anchor, allDay));
                }
                for (LocalDate anchor : List.of(LocalDate.of(2026, 4, 30), LocalDate.of(2027, 2, 28))) {
                    rules.add(Arguments.of(RecurrenceInterval.MONTHLY, count, null, DayRule.LAST_DAY_OF_MONTH, anchor, allDay));
                    rules.add(Arguments.of(RecurrenceInterval.YEARLY, count, null, DayRule.LAST_DAY_OF_MONTH, anchor, allDay));
                }
            }
        }
        return rules.stream();
    }

    @ParameterizedTest(name = "{0} x{1} {2} {3} from {4}, all day: {5}")
    @MethodSource("rules")
    void theExpandedRuleIsTheProjectedOne(RecurrenceInterval interval, int count, Set<DayOfWeek> weekdays, DayRule rule,
                                          LocalDate anchor, boolean allDay) {
        RecurringEventSeries series = series(interval, count, weekdays, rule, anchor, null, null, allDay);
        assertSameOccurrences(series);
        assertSameOccurrences(series(interval, count, weekdays, rule, anchor, anchor.plusMonths(14), null, allDay));
    }

    @ParameterizedTest(name = "carried on: {0} x{1} {2} {3} from {4}")
    @MethodSource("rules")
    void aSeriesCarriedOnIsReadBackFromItsFirstOccurrence(RecurrenceInterval interval, int count, Set<DayOfWeek> weekdays,
                                                          DayRule rule, LocalDate anchor, boolean allDay) {
        assertSameOccurrences(series(interval, count, weekdays, rule, anchor, null, anchor.plusMonths(7).plusDays(3), allDay));
    }

    private void assertSameOccurrences(RecurringEventSeries series) {
        String ics = writer.write(new CalendarFeed("x", TORONTO, List.of(), List.of(new FeedSeries(series, Set.of(), List.of())),
            List.of()), Locale.FRENCH);
        LocalDate from = series.anchorDate();
        LocalDate to = from.plusYears(2);
        List<LocalDateTime> projected = EventRecurrenceProjector.slotsBetween(series, from, to).stream()
            .map(day -> series.allDay() ? day.atStartOfDay() : day.atTime(series.startTime()))
            .toList();
        assertThat(IcalTestSupport.starts(IcalTestSupport.master(ics, series.id() + "@nido"), TORONTO, from, to))
            .isEqualTo(projected);
    }

    private static RecurringEventSeries series(RecurrenceInterval interval, int count, Set<DayOfWeek> weekdays, DayRule rule,
                                               LocalDate anchor, LocalDate end, LocalDate startsOn, boolean allDay) {
        return new RecurringEventSeries(UUID.randomUUID(), UUID.randomUUID(), "Piano", null, null,
            allDay, allDay ? null : LocalTime.of(18, 0), allDay ? null : LocalTime.of(19, 0), 0, null,
            interval, count, weekdays, rule, anchor, end, startsOn, List.of(), UUID.randomUUID(), Instant.now());
    }
}
