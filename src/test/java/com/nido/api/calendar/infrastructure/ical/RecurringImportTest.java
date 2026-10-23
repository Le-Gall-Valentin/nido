package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.model.CalendarEvent;
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

    @ParameterizedTest(name = "at {0}")
    @MethodSource("aroundTheSpringChange")
    void aDailySeriesAtAnHourTheSpringChangeSkipsComesBackAsASeries(LocalTime at) {
        // 29 March 2026 has no 02:30 in Paris: ical4j places that occurrence at 03:30, where nido's series keeps 02:30.
        RecurringEventSeries daily = new RecurringEventSeries(UUID.randomUUID(), UUID.randomUUID(), "Traite", null, null,
            false, at, at.plusMinutes(30), 0, null, RecurrenceInterval.DAILY, 1, LocalDate.of(2026, 3, 20), null,
            List.of(), UUID.randomUUID(), Instant.now());
        String ics = new Ical4jCalendarWriterAdapter().write(new CalendarFeed("x", PARIS, List.of(),
            List.of(new FeedSeries(daily, Set.of(), List.of())), List.of()), Locale.FRENCH);

        ImportGroup group = only(ics, PARIS);

        assertThat(group.unfolded()).isFalse();
        assertThat(group.series().startTime()).isEqualTo(at);
        assertThat(group.series().endDate()).isNull();
    }

    static Stream<LocalTime> aroundTheSpringChange() {
        return Stream.of(LocalTime.of(2, 30), LocalTime.of(3, 30));
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
    void aRuleOfSecondsOrMinutesIsSetAsideAndSaidSo() {
        for (String rule : new String[] {"FREQ=SECONDLY", "FREQ=MINUTELY;INTERVAL=3", "FREQ=SECONDLY;BYMONTH=2"}) {
            ImportReading reading = assertTimeoutPreemptively(Duration.ofSeconds(2), () -> reader.read(ics(vevent("s@test",
                "SUMMARY:Tic", "DTSTART:20261013T080000", "RRULE:" + rule)), PARIS, TODAY, Locale.FRENCH), rule);
            assertThat(reading.groups()).as(rule).isEmpty();
            assertThat(reading.ignored()).as(rule).isEqualTo(1);
            assertThat(reading.notes()).extracting(note -> note.kind(), note -> note.detail()).as(rule)
                .containsExactly(tuple(ImportNoteKind.IGNORED, "TOO_HEAVY"));
        }
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

    private ImportReading read(String content) {
        return assertTimeoutPreemptively(Duration.ofSeconds(5), () -> reader.read(content, PARIS, TODAY, Locale.FRENCH));
    }

    @Test
    void anRdateOutsideTheCheckedWindowOrOffTheSeriesMakesItAnUnfoldedEvent() {
        // A weekly series since 2020 and one date in 2026 that is not a Monday: the series would lose it.
        ImportGroup group = only(ics(vevent("r1@test", "SUMMARY:Cours", "DTSTART;VALUE=DATE:20200106",
            "RRULE:FREQ=WEEKLY;BYDAY=MO", "RDATE;VALUE=DATE:20261021")), PARIS);
        assertThat(group.series()).isNull();
        assertThat(group.unfolded()).isTrue();
        assertThat(group.events()).extracting(ImportedEvent::startDate).contains(LocalDate.of(2026, 10, 21));
    }

    @Test
    void anRdateOnASlotOfTheSeriesIsKeptAsASeriesWhoseEndTheCountAloneGives() {
        ImportedSeries series = only(ics(vevent("r2@test", "SUMMARY:Cours", "DTSTART;VALUE=DATE:20260105",
            "RRULE:FREQ=WEEKLY;COUNT=10", "RDATE;VALUE=DATE:20260112")), PARIS).series();
        assertThat(series).isNotNull();
        assertThat(series.endDate()).isEqualTo(LocalDate.of(2026, 3, 9));
    }

    @Test
    void anRdateAfterTheEndOfACountMakesItUnfolded() {
        ImportGroup group = only(ics(vevent("r3@test", "SUMMARY:Cours", "DTSTART;VALUE=DATE:20260105",
            "RRULE:FREQ=WEEKLY;COUNT=200", "RDATE;VALUE=DATE:20350108")), PARIS);
        assertThat(group.series()).isNull();
        assertThat(group.unfolded()).isTrue();
    }

    @Test
    void moreThanOneRuleIsUnfolded() {
        ImportGroup group = only(ics(vevent("rr@test", "SUMMARY:Deux", "DTSTART;VALUE=DATE:20261012",
            "RRULE:FREQ=WEEKLY;BYDAY=MO;COUNT=3", "RRULE:FREQ=WEEKLY;BYDAY=WE;COUNT=3")), PARIS);
        assertThat(group.series()).isNull();
        assertThat(group.unfolded()).isTrue();
    }

    @Test
    void aSeriesFromAZoneWhoseSummerTimeLeavesTheSpacesLaterIsUnfolded() {
        // 10:00 in New York on the 5th is 16:00 in Paris, but 15:00 on 2027-11-05, when the two zones disagree.
        String monthly = ics(vevent("z1@test", "SUMMARY:Point", "DTSTART;TZID=America/New_York:20230105T100000",
            "DTEND;TZID=America/New_York:20230105T110000", "RRULE:FREQ=MONTHLY"));
        assertThat(only(monthly, PARIS).series()).isNull();
        assertThat(only(monthly, PARIS).unfolded()).isTrue();
        String berlin = ics(vevent("z2@test", "SUMMARY:Point", "DTSTART;TZID=Europe/Berlin:20230105T100000",
            "DTEND;TZID=Europe/Berlin:20230105T110000", "RRULE:FREQ=MONTHLY"));
        assertThat(only(berlin, PARIS).series()).isNotNull();
    }

    @Test
    void aZoneWhoseRulesLeaveTheSpacesOnlySomeYearsAfterItsLastListedChangeIsUnfolded() {
        // A zone of the file's own, as Paris but for its summer time: from the Sunday on or after 24 March rather than
        // the last Sunday of March. The two agree every year but those whose 24 March is a Sunday — 2030 is the first
        // after the changes ical4j lists for it —, when the Mondays of late March come an hour early in Paris.
        String own = "BEGIN:VTIMEZONE\r\nTZID:Bureau\r\n"
            + "BEGIN:DAYLIGHT\r\nTZOFFSETFROM:+0100\r\nTZOFFSETTO:+0200\r\nDTSTART:19960324T020000\r\n"
            + "RRULE:FREQ=YEARLY;BYMONTH=3;BYMONTHDAY=24;BYDAY=SU\r\nEND:DAYLIGHT\r\n"
            + "BEGIN:STANDARD\r\nTZOFFSETFROM:+0200\r\nTZOFFSETTO:+0100\r\nDTSTART:19961027T030000\r\n"
            + "RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU\r\nEND:STANDARD\r\nEND:VTIMEZONE\r\n";
        String content = ics(own + vevent("own@test", "SUMMARY:Point", "DTSTART;TZID=Bureau:20261102T100000",
            "DTEND;TZID=Bureau:20261102T110000", "RRULE:FREQ=WEEKLY;BYDAY=MO;UNTIL=20341231T000000Z"));

        ImportGroup group = only(content, PARIS);

        assertThat(group.series()).isNull();
        assertThat(group.unfolded()).isTrue();
    }

    @Test
    void anExdateOfAnotherTimeOfTheDayCancelsNothing() {
        ImportedSeries series = only(ics(vevent("x1@test", "SUMMARY:Piano", "DTSTART;TZID=Europe/Paris:20260105T180000",
            "DTEND;TZID=Europe/Paris:20260105T190000", "RRULE:FREQ=WEEKLY;BYDAY=MO",
            "EXDATE;TZID=Europe/Paris:20260112T090000")), PARIS).series();
        assertThat(series).isNotNull();
        assertThat(series.cancelledSlots()).isEmpty();
    }

    @Test
    void anExruleIsAppliedAndMakesTheEventUnfolded() {
        ImportGroup group = only(ics(vevent("xr@test", "SUMMARY:Cours", "DTSTART;VALUE=DATE:20261012",
            "RRULE:FREQ=WEEKLY;BYDAY=MO;COUNT=5", "EXRULE:FREQ=WEEKLY;BYDAY=MO;COUNT=2")), PARIS);
        assertThat(group.series()).isNull();
        assertThat(group.events()).extracting(ImportedEvent::startDate)
            .containsExactly(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 9));
    }

    @Test
    void aFirstDayOffItsRuleIsAnOccurrenceOfTheUnfoldedEventToo() {
        ImportGroup group = only(ics(vevent("m3@test", "SUMMARY:Réunion", "DTSTART;VALUE=DATE:20260701",
            "RRULE:FREQ=MONTHLY;BYDAY=5TU;COUNT=2")), PARIS);
        assertThat(group.unfolded()).isTrue();
        assertThat(group.events()).extracting(ImportedEvent::startDate)
            .containsExactly(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 29), LocalDate.of(2026, 12, 29));
    }

    @Test
    void aRuleWhoseOnlyOccurrenceInTheWindowIsExcludedLeavesNothing() {
        ImportReading reading = read(ics(vevent("m4@test", "SUMMARY:Rare", "DTSTART;VALUE=DATE:20260301",
            "RRULE:FREQ=YEARLY;INTERVAL=3", "EXDATE;VALUE=DATE:20260301")));
        assertThat(reading.groups()).isEmpty();
        assertThat(reading.ignored()).isZero();
        assertThat(reading.notes()).isEmpty();
    }

    @Test
    void aCountSeriesEndsOnTheDayTheProjectorGivesItsLastOccurrence() {
        ImportedSeries series = only(ics(vevent("e@test", "SUMMARY:Cours", "DTSTART;VALUE=DATE:20260105",
            "RRULE:FREQ=WEEKLY;COUNT=200")), PARIS).series();
        assertThat(series.endDate()).isEqualTo(LocalDate.of(2026, 1, 5).plusWeeks(199));
    }

    @Test
    void anUnfoldedRuleWhoseOccurrencesAllLieOutsideTheWindowIsStillSaid() {
        ImportReading reading = reader.read(ics(vevent("p@test", "SUMMARY:Réunion", "DTSTART;VALUE=DATE:20150331",
            "RRULE:FREQ=MONTHLY;BYDAY=5TU;COUNT=3")), PARIS, TODAY, Locale.FRENCH);
        assertThat(reading.notes()).extracting(note -> note.kind(), note -> note.detail())
            .containsExactly(tuple(ImportNoteKind.UNFOLDED_PARTIAL, "0"));
    }

    @Test
    void aCountCountsAnExcludedFirstOccurrenceAndKeepsItsSlotCancelled() {
        ImportedSeries series = only(ics(vevent("b3@test", "SUMMARY:Cours", "DTSTART;VALUE=DATE:20260105",
            "RRULE:FREQ=WEEKLY;COUNT=200", "EXDATE;VALUE=DATE:20260105")), PARIS).series();
        assertThat(series.endDate()).isEqualTo(LocalDate.of(2029, 10, 29));
        assertThat(series.cancelledSlots()).containsExactly(LocalDate.of(2026, 1, 5));
    }

    @Test
    void aCountWhoseFirstTwoOccurrencesAreExcludedEndsOnTheSameDay() {
        ImportedSeries series = only(ics(vevent("b3b@test", "SUMMARY:Cours", "DTSTART;VALUE=DATE:20260105",
            "RRULE:FREQ=WEEKLY;COUNT=200", "EXDATE;VALUE=DATE:20260105,20260112")), PARIS).series();
        assertThat(series.endDate()).isEqualTo(LocalDate.of(2029, 10, 29));
        assertThat(series.cancelledSlots()).containsExactlyInAnyOrder(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 12));
    }

    private static final String WEEKLY_PIANO = vevent("o@test", "SUMMARY:Piano", "DTSTART;TZID=Europe/Paris:20260105T180000",
        "DTEND;TZID=Europe/Paris:20260105T190000", "RRULE:FREQ=WEEKLY;BYDAY=MO");

    @Test
    void anOverrideNidoCannotHoldIsSaidAndItsSlotKeepsTheOccurrence() {
        String tooLong = vevent("o@test", "SUMMARY:Absurde", "RECURRENCE-ID;TZID=Europe/Paris:20260119T180000",
            "DTSTART;TZID=Europe/Paris:20260119T180000", "DURATION:P999999999D");
        ImportReading reading = read(ics(WEEKLY_PIANO, tooLong));
        ImportedSeries series = reading.groups().getFirst().series();
        assertThat(series.editedOccurrences()).isEmpty();
        assertThat(series.cancelledSlots()).isEmpty();
        assertThat(reading.ignored()).isEqualTo(1);
        assertThat(reading.notes()).extracting(note -> note.kind(), note -> note.detail())
            .containsExactly(tuple(ImportNoteKind.IGNORED, "TOO_LONG"));
    }

    @Test
    void aRecurrenceIdInUtcFindsItsSlotInAZonedSeries() {
        // 18:00 in Paris in January is 17:00 UTC.
        String moved = vevent("o@test", "SUMMARY:Piano (salle 3)", "RECURRENCE-ID:20260119T170000Z",
            "DTSTART;TZID=Europe/Paris:20260119T200000", "DTEND;TZID=Europe/Paris:20260119T210000");
        ImportedSeries series = only(ics(WEEKLY_PIANO, moved), PARIS).series();
        assertThat(series.editedOccurrences()).singleElement().satisfies(edited -> {
            assertThat(edited.slot()).isEqualTo(LocalDate.of(2026, 1, 19));
            assertThat(edited.event().startTime()).isEqualTo(LocalTime.of(20, 0));
        });
    }

    @Test
    void anOverrideOfADayThatIsNoSlotIsAnEventOfItsOwn() {
        String other = vevent("o@test", "SUMMARY:Rattrapage", "RECURRENCE-ID;TZID=Europe/Paris:20260120T180000",
            "DTSTART;TZID=Europe/Paris:20260121T180000", "DTEND;TZID=Europe/Paris:20260121T190000");
        ImportGroup group = only(ics(WEEKLY_PIANO, other), PARIS);
        assertThat(group.series().editedOccurrences()).isEmpty();
        assertThat(group.events()).extracting(ImportedEvent::title).containsExactly("Rattrapage");
    }

    @Test
    void anEditedOccurrenceGivenTwiceKeepsItsLatestVersionAndSaysTheOther() {
        // Two versions of the 19 January occurrence, the newer one second and its RECURRENCE-ID written in UTC.
        String older = vevent("o@test", "SUMMARY:Piano (salle 2)", "SEQUENCE:1", "RECURRENCE-ID;TZID=Europe/Paris:20260119T180000",
            "DTSTART;TZID=Europe/Paris:20260119T190000", "DTEND;TZID=Europe/Paris:20260119T200000");
        String newer = vevent("o@test", "SUMMARY:Piano (salle 3)", "SEQUENCE:2", "RECURRENCE-ID:20260119T170000Z",
            "DTSTART;TZID=Europe/Paris:20260119T200000", "DTEND;TZID=Europe/Paris:20260119T210000");
        ImportReading reading = read(ics(WEEKLY_PIANO, newer, older));
        ImportedSeries series = reading.groups().getFirst().series();
        assertThat(series.editedOccurrences()).singleElement().satisfies(edited -> {
            assertThat(edited.slot()).isEqualTo(LocalDate.of(2026, 1, 19));
            assertThat(edited.event().title()).isEqualTo("Piano (salle 3)");
        });
        assertThat(reading.ignored()).isEqualTo(1);
        assertThat(reading.notes()).extracting(note -> note.kind(), note -> note.title(), note -> note.detail())
            .containsExactly(tuple(ImportNoteKind.IGNORED, "Piano (salle 2)", "DUPLICATE_UID"));
    }

    @Test
    void anEditedOccurrenceRepeatedAsItIsKeepsTheFirst() {
        String first = vevent("o@test", "SUMMARY:Piano (salle 2)", "RECURRENCE-ID;TZID=Europe/Paris:20260119T180000",
            "DTSTART;TZID=Europe/Paris:20260119T190000", "DTEND;TZID=Europe/Paris:20260119T200000");
        String again = vevent("o@test", "SUMMARY:Piano (copie)", "RECURRENCE-ID;TZID=Europe/Paris:20260119T180000",
            "DTSTART;TZID=Europe/Paris:20260119T190000", "DTEND;TZID=Europe/Paris:20260119T200000");
        ImportReading reading = read(ics(WEEKLY_PIANO, first, again));
        assertThat(reading.groups().getFirst().series().editedOccurrences())
            .extracting(edited -> edited.event().title()).containsExactly("Piano (salle 2)");
        assertThat(reading.notes()).extracting(note -> note.title(), note -> note.detail())
            .containsExactly(tuple("Piano (copie)", "DUPLICATE_UID"));
    }

    @Test
    void anEditedOccurrenceGetsTheUsualNotes() {
        String edited = vevent("o@test", "SUMMARY:" + "x".repeat(250), "RECURRENCE-ID;TZID=Europe/Paris:20260119T180000",
            "DTSTART;TZID=Europe/Paris:20260119T200000", "DTEND;TZID=Europe/Paris:20260119T210000");
        ImportReading reading = read(ics(WEEKLY_PIANO, edited));
        assertThat(reading.notes()).extracting(note -> note.kind(), note -> note.detail())
            .containsExactly(tuple(ImportNoteKind.TRUNCATED_TEXT, "title"));
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

    @Test
    void aNidoExportOfAnAllDaySeriesComesBackWithItsCancelledAndEditedOccurrences() {
        UUID seriesId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        RecurringEventSeries written = new RecurringEventSeries(seriesId, spaceId, "Marché", null, null,
            true, null, null, 0, null, RecurrenceInterval.WEEKLY, 1, Set.of(DayOfWeek.SATURDAY), null,
            LocalDate.of(2026, 1, 3), LocalDate.of(2026, 3, 28), null, List.of(), UUID.randomUUID(), Instant.now());
        CalendarEvent moved = new CalendarEvent(UUID.randomUUID(), spaceId, "Marché (place)", null, null, true,
            LocalDate.of(2026, 1, 18), null, LocalDate.of(2026, 1, 18), null, null, List.of(), seriesId,
            LocalDate.of(2026, 1, 17), UUID.randomUUID(), Instant.now());
        String ics = new Ical4jCalendarWriterAdapter().write(new CalendarFeed("x", PARIS, List.of(),
            List.of(new FeedSeries(written, Set.of(LocalDate.of(2026, 1, 10)), List.of(moved))), List.of()), Locale.FRENCH);

        ImportGroup group = only(ics, PARIS);

        assertThat(group.events()).isEmpty();
        ImportedSeries read = group.series();
        assertThat(read.allDay()).isTrue();
        assertThat(read.weekdays()).containsExactly(DayOfWeek.SATURDAY);
        assertThat(read.anchorDate()).isEqualTo(LocalDate.of(2026, 1, 3));
        assertThat(read.endDate()).isEqualTo(LocalDate.of(2026, 3, 28));
        assertThat(read.cancelledSlots()).containsExactly(LocalDate.of(2026, 1, 10));
        assertThat(read.editedOccurrences()).singleElement().satisfies(edited -> {
            assertThat(edited.slot()).isEqualTo(LocalDate.of(2026, 1, 17));
            assertThat(edited.event().title()).isEqualTo("Marché (place)");
            assertThat(edited.event().allDay()).isTrue();
            assertThat(edited.event().startDate()).isEqualTo(LocalDate.of(2026, 1, 18));
            assertThat(edited.event().endDate()).isEqualTo(LocalDate.of(2026, 1, 18));
        });
    }
}
