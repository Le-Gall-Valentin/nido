package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.model.CalendarFeed;
import com.nido.api.calendar.domain.model.DayRule;
import com.nido.api.calendar.domain.model.FeedSeries;
import com.nido.api.calendar.domain.model.ImportGroup;
import com.nido.api.calendar.domain.model.ImportNoteKind;
import com.nido.api.calendar.domain.model.ImportReading;
import com.nido.api.calendar.domain.model.ImportedEvent;
import com.nido.api.calendar.domain.model.ImportedSeries;
import com.nido.api.calendar.domain.model.RecurrenceInterval;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.nido.api.calendar.infrastructure.ical.Ical4jImportReaderAdapterTest.PARIS;
import static com.nido.api.calendar.infrastructure.ical.Ical4jImportReaderAdapterTest.TODAY;
import static com.nido.api.calendar.infrastructure.ical.Ical4jImportReaderAdapterTest.ics;
import static com.nido.api.calendar.infrastructure.ical.Ical4jImportReaderAdapterTest.vevent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class RecurringImportTest {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private final Ical4jImportReaderAdapter reader = new Ical4jImportReaderAdapter();

    private ImportGroup only(String content, ZoneId zone) {
        ImportReading reading = reader.read(content, zone, TODAY, Locale.FRENCH);
        assertThat(reading.groups()).hasSize(1);
        return reading.groups().getFirst();
    }

    @Test
    void aWeeklyRuleOnTwoDaysIsASeries() {
        ImportGroup group = only(ics(vevent("w@test", "SUMMARY:Piano",
            "DTSTART;TZID=Europe/Paris:20260105T180000", "DTEND;TZID=Europe/Paris:20260105T190000",
            "RRULE:FREQ=WEEKLY;BYDAY=MO,TH")), PARIS);

        assertThat(group.unfolded()).isFalse();
        assertThat(group.events()).isEmpty();
        ImportedSeries series = group.series();
        assertThat(series.intervalType()).isEqualTo(RecurrenceInterval.WEEKLY);
        assertThat(series.weekdays()).containsExactly(DayOfWeek.MONDAY, DayOfWeek.THURSDAY);
        assertThat(series.anchorDate()).isEqualTo(LocalDate.of(2026, 1, 5));
        assertThat(series.startTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(series.endTime()).isEqualTo(LocalTime.of(19, 0));
        assertThat(series.endDate()).isNull();
    }

    @Test
    void aCountBecomesTheDayOfTheLastOccurrence() {
        ImportedSeries series = only(ics(vevent("c@test", "SUMMARY:Cours", "DTSTART;VALUE=DATE:20260105",
            "RRULE:FREQ=WEEKLY;COUNT=10")), PARIS).series();
        assertThat(series.endDate()).isEqualTo(LocalDate.of(2026, 3, 9));
    }

    @Test
    void anUntilBeforeTheTimeOfTheLastDayEndsTheDayBefore() {
        // UNTIL 2026-03-09 08:00 UTC is 09:00 in Paris, before the 18:00 occurrence of that Monday.
        ImportedSeries series = only(ics(vevent("u@test", "SUMMARY:Piano",
            "DTSTART;TZID=Europe/Paris:20260105T180000", "DTEND;TZID=Europe/Paris:20260105T190000",
            "RRULE:FREQ=WEEKLY;UNTIL=20260309T080000Z")), PARIS).series();
        assertThat(series.endDate()).isEqualTo(LocalDate.of(2026, 3, 8));
    }

    @Test
    void cancellationsAndEditedOccurrencesFollowTheSeries() {
        String master = vevent("p@test", "SUMMARY:Piano",
            "DTSTART;TZID=Europe/Paris:20260105T180000", "DTEND;TZID=Europe/Paris:20260105T190000",
            "RRULE:FREQ=WEEKLY;BYDAY=MO", "EXDATE;TZID=Europe/Paris:20260112T180000");
        String moved = vevent("p@test", "SUMMARY:Piano (salle 3)", "RECURRENCE-ID;TZID=Europe/Paris:20260119T180000",
            "DTSTART;TZID=Europe/Paris:20260120T180000", "DTEND;TZID=Europe/Paris:20260120T190000");
        String cancelled = vevent("p@test", "SUMMARY:Piano", "STATUS:CANCELLED", "RECURRENCE-ID;TZID=Europe/Paris:20260126T180000",
            "DTSTART;TZID=Europe/Paris:20260126T180000", "DTEND;TZID=Europe/Paris:20260126T190000");

        ImportedSeries series = only(ics(master, moved, cancelled), PARIS).series();

        assertThat(series.cancelledSlots()).containsExactlyInAnyOrder(LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 26));
        assertThat(series.editedOccurrences()).singleElement().satisfies(edited -> {
            assertThat(edited.slot()).isEqualTo(LocalDate.of(2026, 1, 19));
            assertThat(edited.event().title()).isEqualTo("Piano (salle 3)");
            assertThat(edited.event().startDate()).isEqualTo(LocalDate.of(2026, 1, 20));
        });
    }

    @Test
    void aFirstDayOffItsRuleIsAnEventOfItsOwn() {
        // DTSTART on a Wednesday, the rule on Mondays: the series starts on the next Monday.
        ImportGroup group = only(ics(vevent("o@test", "SUMMARY:Atelier", "DTSTART;VALUE=DATE:20260107",
            "RRULE:FREQ=WEEKLY;BYDAY=MO")), PARIS);
        assertThat(group.series().anchorDate()).isEqualTo(LocalDate.of(2026, 1, 12));
        assertThat(group.events()).extracting(ImportedEvent::startDate).containsExactly(LocalDate.of(2026, 1, 7));
    }

    @Test
    void aFifthTuesdayIsUnfolded() {
        ImportGroup group = only(ics(vevent("f@test", "SUMMARY:Réunion", "DTSTART;VALUE=DATE:20260331",
            "RRULE:FREQ=MONTHLY;BYDAY=5TU;COUNT=3")), PARIS);
        assertThat(group.series()).isNull();
        assertThat(group.unfolded()).isTrue();
        assertThat(group.events()).extracting(ImportedEvent::startDate)
            .containsExactly(LocalDate.of(2026, 3, 31), LocalDate.of(2026, 6, 30), LocalDate.of(2026, 9, 29));
    }

    @Test
    void aMonthlyThirtyFirstSkippingShortMonthsIsUnfolded() {
        // RFC 5545 skips the months without a 31st; nido would cap them at their last day.
        ImportGroup group = only(ics(vevent("m@test", "SUMMARY:Paie", "DTSTART;VALUE=DATE:20260131",
            "RRULE:FREQ=MONTHLY;COUNT=4")), PARIS);
        assertThat(group.unfolded()).isTrue();
        assertThat(group.events()).extracting(ImportedEvent::startDate).containsExactly(LocalDate.of(2026, 1, 31),
            LocalDate.of(2026, 3, 31), LocalDate.of(2026, 5, 31), LocalDate.of(2026, 7, 31));
    }

    @Test
    void anHourlyRuleIsUnfoldedOnItsWindowAndSaidPartial() {
        ImportReading reading = reader.read(ics(vevent("h@test", "SUMMARY:Pilule", "DTSTART:20261013T080000",
            "DTEND:20261013T081500", "RRULE:FREQ=HOURLY;INTERVAL=2")), PARIS, TODAY, Locale.FRENCH);
        ImportGroup group = reading.groups().getFirst();
        assertThat(group.events()).hasSize(500);
        assertThat(reading.notes()).extracting(note -> note.kind()).containsExactly(ImportNoteKind.UNFOLDED_PARTIAL);
    }

    @Test
    void a_series_whose_zone_shifts_its_hours_is_unfolded() {
        // Mondays at 10:00 in New York: 16:00 in Paris, but 15:00 for the three weeks the United States move to
        // summer time before Europe does — no nido series says that.
        String content = ics(vevent("ny@test", "SUMMARY:Point US", "DTSTART;TZID=America/New_York:20260105T100000",
            "DTEND;TZID=America/New_York:20260105T110000", "RRULE:FREQ=WEEKLY;BYDAY=MO"));

        ImportGroup inParis = only(content, PARIS);
        assertThat(inParis.series()).isNull();
        assertThat(inParis.events()).extracting(event -> LocalDateTime.of(event.startDate(), event.startTime()))
            .contains(LocalDateTime.of(2026, 3, 9, 15, 0), LocalDateTime.of(2026, 3, 30, 16, 0));

        ImportGroup inNewYork = only(content, NEW_YORK);
        assertThat(inNewYork.series()).as("in its own zone, the same rule is a series").isNotNull();
    }

    @Test
    void everyWorkingDayIsAWeeklySeriesOnFiveDays() {
        ImportedSeries series = only(ics(vevent("wd@test", "SUMMARY:Cantine", "DTSTART;VALUE=DATE:20260105",
            "RRULE:FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR")), PARIS).series();
        assertThat(series.weekdays()).hasSize(5);
    }

    @Test
    void aRuleOfSecondsOrMinutesIsUnfoldedOnItsFirstOccurrencesWithoutExpandingTheWindow() {
        for (String rule : new String[] {"FREQ=SECONDLY", "FREQ=MINUTELY;INTERVAL=3"}) {
            ImportReading reading = assertTimeoutPreemptively(Duration.ofSeconds(10), () -> reader.read(ics(vevent("s@test",
                "SUMMARY:Tic", "DTSTART:20261013T080000", "RRULE:" + rule)), PARIS, TODAY, Locale.FRENCH), rule);
            ImportGroup group = reading.groups().getFirst();
            assertThat(group.unfolded()).as(rule).isTrue();
            assertThat(group.events().size()).as(rule).isBetween(0, 500);
            assertThat(reading.notes()).extracting(note -> note.kind()).as(rule).containsExactly(ImportNoteKind.UNFOLDED_PARTIAL);
        }
    }

    @Test
    void aRuleOfSecondsThatIsFarFromItsFirstHitComesBackAtOnceAsItsFirstDay() {
        // ical4j gives up after a thousand empty seconds in a row, which is what bounds its work here: the rule gives
        // nothing it found, and what is left of it is its first day.
        ImportReading reading = assertTimeoutPreemptively(Duration.ofSeconds(10), () -> reader.read(ics(vevent("b@test",
            "SUMMARY:Tic", "DTSTART:20261013T080000", "RRULE:FREQ=SECONDLY;BYMONTH=2")), PARIS, TODAY, Locale.FRENCH));
        assertThat(reading.groups().getFirst().events()).extracting(ImportedEvent::startDate).containsExactly(LocalDate.of(2026, 10, 13));
    }

    @Test
    void aCountToFollowFromFarBackAtASecondRhythmIsSetAsideAndSaidSo() {
        ImportReading reading = assertTimeoutPreemptively(Duration.ofSeconds(10), () -> reader.read(ics(vevent("k@test",
            "SUMMARY:Pouls", "DTSTART:20000101T000000", "RRULE:FREQ=SECONDLY;COUNT=2000000000")), PARIS, TODAY, Locale.FRENCH));
        assertThat(reading.groups()).isEmpty();
        assertThat(reading.ignored()).isEqualTo(1);
        assertThat(reading.notes()).extracting(note -> note.kind(), note -> note.detail())
            .containsExactly(tuple(ImportNoteKind.IGNORED, "TOO_HEAVY"));
    }

    @Test
    void aRuleGivingMillionsOfInstantsAPeriodIsSetAsideAndSaidSo() {
        String hours = IntStream.range(0, 24).mapToObj(String::valueOf).collect(Collectors.joining(","));
        String minutes = IntStream.range(0, 60).mapToObj(String::valueOf).collect(Collectors.joining(","));
        String days = IntStream.rangeClosed(1, 366).mapToObj(String::valueOf).collect(Collectors.joining(","));
        ImportReading reading = assertTimeoutPreemptively(Duration.ofSeconds(10), () -> reader.read(ics(vevent("y@test",
            "SUMMARY:Tout", "DTSTART:20261013T080000",
            "RRULE:FREQ=YEARLY;BYYEARDAY=" + days + ";BYHOUR=" + hours + ";BYMINUTE=" + minutes + ";BYSECOND=" + minutes)),
            PARIS, TODAY, Locale.FRENCH));
        assertThat(reading.groups()).isEmpty();
        assertThat(reading.notes()).extracting(note -> note.detail()).containsExactly("TOO_HEAVY");
    }

    @Test
    void anExdateAndAnRdateAreFollowedOnAnEventThatIsNotARuleNidoSays() {
        ImportGroup group = only(ics(vevent("r@test", "SUMMARY:Dates", "DTSTART;VALUE=DATE:20261020",
            "RDATE;VALUE=DATE:20261027,20261103,20261110", "EXDATE;VALUE=DATE:20261103")), PARIS);
        assertThat(group.unfolded()).isTrue();
        assertThat(group.events()).extracting(ImportedEvent::startDate)
            .containsExactly(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 27), LocalDate.of(2026, 11, 10));
    }

    static Stream<Arguments> nidoRules() {
        return Stream.of(
            Arguments.of(RecurrenceInterval.DAILY, 2, null, null, LocalDate.of(2026, 1, 31)),
            Arguments.of(RecurrenceInterval.WEEKLY, 1, Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), null, LocalDate.of(2026, 1, 5)),
            Arguments.of(RecurrenceInterval.WEEKLY, 3, Set.of(DayOfWeek.SATURDAY), null, LocalDate.of(2026, 1, 10)),
            Arguments.of(RecurrenceInterval.MONTHLY, 1, null, DayRule.SAME_DAY, LocalDate.of(2026, 1, 30)),
            Arguments.of(RecurrenceInterval.MONTHLY, 1, null, DayRule.SAME_DAY, LocalDate.of(2026, 1, 31)),
            Arguments.of(RecurrenceInterval.MONTHLY, 2, null, DayRule.WEEKDAY_OF_MONTH, LocalDate.of(2026, 10, 13)),
            Arguments.of(RecurrenceInterval.MONTHLY, 1, null, DayRule.LAST_WEEKDAY_OF_MONTH, LocalDate.of(2026, 10, 30)),
            Arguments.of(RecurrenceInterval.MONTHLY, 1, null, DayRule.LAST_DAY_OF_MONTH, LocalDate.of(2026, 4, 30)),
            Arguments.of(RecurrenceInterval.YEARLY, 1, null, DayRule.SAME_DAY, LocalDate.of(2028, 2, 29)),
            Arguments.of(RecurrenceInterval.YEARLY, 1, null, DayRule.WEEKDAY_OF_MONTH, LocalDate.of(2026, 11, 26)),
            Arguments.of(RecurrenceInterval.YEARLY, 1, null, DayRule.LAST_WEEKDAY_OF_MONTH, LocalDate.of(2026, 5, 31)));
    }

    @ParameterizedTest(name = "{0} x{1} {2} {3} from {4}")
    @MethodSource("nidoRules")
    void aNidoExportIsReadBackAsTheSameSeries(RecurrenceInterval interval, int count, Set<DayOfWeek> weekdays,
                                              DayRule rule, LocalDate anchor) {
        RecurringEventSeries written = new RecurringEventSeries(UUID.randomUUID(), UUID.randomUUID(), "Piano", null, null,
            false, LocalTime.of(18, 0), LocalTime.of(19, 0), 0, null, interval, count, weekdays, rule, anchor,
            anchor.plusMonths(20), null, List.of(), UUID.randomUUID(), Instant.now());
        String ics = new Ical4jCalendarWriterAdapter().write(new CalendarFeed("x", PARIS, List.of(),
            List.of(new FeedSeries(written, Set.of(), List.of())), List.of()), Locale.FRENCH);

        ImportedSeries read = only(ics, PARIS).series();

        assertThat(read).isNotNull();
        assertThat(read.intervalType()).isEqualTo(written.intervalType());
        assertThat(read.intervalCount()).isEqualTo(written.intervalCount());
        assertThat(read.weekdays()).isEqualTo(written.weekdays());
        assertThat(read.dayRule()).isEqualTo(written.dayRule());
        assertThat(read.anchorDate()).isEqualTo(written.anchorDate());
        assertThat(read.endDate()).isEqualTo(written.endDate());
    }
}
