package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.model.ImportNote;
import com.nido.api.calendar.domain.model.ImportNoteKind;
import com.nido.api.calendar.domain.model.ImportReading;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.nido.api.calendar.infrastructure.ical.Ical4jImportReaderAdapterTest.PARIS;
import static com.nido.api.calendar.infrastructure.ical.Ical4jImportReaderAdapterTest.TODAY;
import static com.nido.api.calendar.infrastructure.ical.Ical4jImportReaderAdapterTest.ics;
import static com.nido.api.calendar.infrastructure.ical.Ical4jImportReaderAdapterTest.vevent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Hostile rules, through the reader: each must come back within two seconds, the event set aside and the reason said.
 * (A rule that sent ical4j into an endless loop cannot be interrupted: these run under a preemptive timeout.)
 */
class BoundedExpansionTest {

    private final Ical4jImportReaderAdapter reader = new Ical4jImportReaderAdapter();

    private ImportReading read(String... events) {
        return assertTimeoutPreemptively(Duration.ofSeconds(2), () -> reader.read(ics(events), PARIS, TODAY, Locale.FRENCH));
    }

    private static void setAside(ImportReading reading, String detail) {
        assertThat(reading.groups()).isEmpty();
        assertThat(reading.ignored()).isEqualTo(1);
        assertThat(reading.notes()).extracting(ImportNote::kind, ImportNote::detail)
            .containsExactly(org.assertj.core.groups.Tuple.tuple(ImportNoteKind.IGNORED, detail));
    }

    private static String list(int from, int to) {
        return IntStream.rangeClosed(from, to).mapToObj(String::valueOf).collect(Collectors.joining(","));
    }

    @Test
    void aRuleOfSecondsFromBefore1994WouldOverflowIcal4jsMultiplierAndIsRefused() {
        setAside(read(vevent("c1@test", "SUMMARY:Tic", "DTSTART:19800101T000000", "RRULE:FREQ=SECONDLY")), "TOO_HEAVY");
    }

    @Test
    void anIntervalOfAThousandMillionYearsIsABadRule() {
        setAside(read(vevent("c2@test", "SUMMARY:Long", "DTSTART;VALUE=DATE:19000101", "RRULE:FREQ=YEARLY;INTERVAL=1073741824")),
            "BAD_RULE");
    }

    @Test
    void aDateStartWithAHourlyRuleIsABadRule() {
        setAside(read(vevent("i1@test", "SUMMARY:Pilule", "DTSTART;VALUE=DATE:20261013", "RRULE:FREQ=HOURLY")), "BAD_RULE");
    }

    @Test
    void aDateStartWithAHourPartIsABadRule() {
        setAside(read(vevent("i2@test", "SUMMARY:Pilule", "DTSTART;VALUE=DATE:20261013", "RRULE:FREQ=DAILY;BYHOUR=9")), "BAD_RULE");
    }

    @Test
    void anIntervalIcal4jCannotAddIsABadRule() {
        setAside(read(vevent("i3@test", "SUMMARY:Long", "DTSTART;VALUE=DATE:20261013", "RRULE:FREQ=YEARLY;INTERVAL=2000000000")),
            "BAD_RULE");
    }

    @Test
    void aSecondRuleWalkingHalfAMillionMinutesIsRefused() {
        setAside(read(vevent("h1@test", "SUMMARY:Tic", "DTSTART:20100101T000000",
            "RRULE:FREQ=SECONDLY;BYMINUTE=0,16,32,48;BYSECOND=0;COUNT=499999")), "TOO_HEAVY");
    }

    @Test
    void aYearlyRuleBuildingMillionsOfCandidatesIsRefused() {
        setAside(read(vevent("h2@test", "SUMMARY:Tout", "DTSTART:15000101T000000",
            "RRULE:FREQ=YEARLY;BYDAY=MO,TU,WE,TH,FR,SA,SU;BYHOUR=" + list(0, 23) + ";BYMINUTE=0,15,30,45;BYSETPOS=1;COUNT=578")),
            "TOO_HEAVY");
    }

    @Test
    void theBudgetIsOneForTheWholeFile() {
        // Each rule costs about two million candidates: one fits the file's budget, the 299 others are refused at once.
        String rule = "RRULE:FREQ=YEARLY;BYYEARDAY=" + list(1, 366) + ";BYHOUR=" + list(0, 9) + ";COUNT=100000";
        String[] copies = IntStream.range(0, 300).mapToObj(i -> vevent("b" + i + "@test", "SUMMARY:Copie " + i,
            "DTSTART:15000101T000000", rule)).toArray(String[]::new);
        ImportReading reading = assertTimeoutPreemptively(Duration.ofSeconds(2),
            () -> reader.read(ics(copies), PARIS, TODAY, Locale.FRENCH));
        assertThat(reading.ignored()).isGreaterThanOrEqualTo(298);
        assertThat(reading.notes()).filteredOn(note -> note.kind() == ImportNoteKind.IGNORED)
            .extracting(ImportNote::detail).containsOnly("TOO_HEAVY");
    }

    @Test
    void aListRepeatingOneDayTwentyThousandTimesIsABadRule() {
        setAside(read(vevent("d1@test", "SUMMARY:Lundis", "DTSTART:15000101T000000",
            "RRULE:FREQ=YEARLY;BYDAY=" + "MO,".repeat(19_999) + "MO;COUNT=100000")), "BAD_RULE");
    }

    @Test
    void aRuleOfEveryYearDayEveryMonthDayEveryMinuteIsTooHeavy() {
        setAside(read(vevent("d2@test", "SUMMARY:Tout", "DTSTART:15000101T000000",
            "RRULE:FREQ=YEARLY;BYYEARDAY=" + list(1, 366) + ";BYMONTHDAY=" + list(1, 31) + ";BYHOUR=" + list(0, 23)
                + ";BYMINUTE=" + list(0, 59))), "TOO_HEAVY");
    }

    @Test
    void aRepeatedMonthDayIsABadRule() {
        setAside(read(vevent("d3@test", "SUMMARY:Fin", "DTSTART;VALUE=DATE:20261031", "RRULE:FREQ=DAILY;BYMONTHDAY=31,31")), "BAD_RULE");
    }

    @Test
    void aThousandOrdinaryCountSeriesAreAllReadWithinTheBudget() {
        String[] events = IntStream.range(0, 1_000).mapToObj(i -> i % 2 == 0
            ? vevent("n" + i + "@test", "SUMMARY:Court " + i, "DTSTART;VALUE=DATE:20180101", "RRULE:FREQ=DAILY;COUNT=5")
            : vevent("n" + i + "@test", "SUMMARY:Hebdo " + i, "DTSTART;VALUE=DATE:20150105", "RRULE:FREQ=WEEKLY;BYDAY=MO;COUNT=10"))
            .toArray(String[]::new);
        ImportReading reading = assertTimeoutPreemptively(Duration.ofSeconds(2),
            () -> reader.read(ics(events), PARIS, TODAY, Locale.FRENCH));
        assertThat(reading.ignored()).isZero();
        assertThat(reading.groups()).hasSize(1_000).allSatisfy(group -> assertThat(group.series()).isNotNull());
    }
}
