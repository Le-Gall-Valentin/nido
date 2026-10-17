package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.model.ExternalFeed;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class Ical4jFeedReaderAdapterTest {

    private final Ical4jFeedReaderAdapter reader = new Ical4jFeedReaderAdapter();

    @Test
    void aCalendarGivesTheNameItCallsItself() {
        assertThat(reader.calendarName("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:x\r\nX-WR-CALNAME:Vacances scolaires\r\nEND:VCALENDAR\r\n"))
            .contains("Vacances scolaires");
        assertThat(reader.calendarName("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:x\r\nEND:VCALENDAR\r\n")).isEmpty();
    }

    @Test
    void anythingElseIsNotACalendar() {
        assertThatThrownBy(() -> reader.calendarName("<html>Moved</html>")).isInstanceOf(CalendarException.NotAnIcalFile.class);
    }

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final String FEED = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:x\r\n"
        + "BEGIN:VEVENT\r\nUID:toussaint\r\nDTSTAMP:20260901T000000Z\r\nSUMMARY:Vacances de la Toussaint\r\n"
        + "DTSTART;VALUE=DATE:20261017\r\nDTEND;VALUE=DATE:20261102\r\nEND:VEVENT\r\n"
        + "BEGIN:VEVENT\r\nUID:club\r\nDTSTAMP:20260901T000000Z\r\nSUMMARY:Club\r\nDESCRIPTION:<b>Maillots</b>\r\n"
        + "DTSTART;TZID=Europe/Paris:20261005T180000\r\nDTEND;TZID=Europe/Paris:20261005T190000\r\n"
        + "RRULE:FREQ=WEEKLY;BYDAY=MO\r\nEXDATE;TZID=Europe/Paris:20261019T180000\r\nEND:VEVENT\r\n"
        + "BEGIN:VEVENT\r\nUID:club\r\nDTSTAMP:20260901T000000Z\r\nSUMMARY:Club (gymnase B)\r\n"
        + "RECURRENCE-ID;TZID=Europe/Paris:20261026T180000\r\n"
        + "DTSTART;TZID=Europe/Paris:20261027T180000\r\nDTEND;TZID=Europe/Paris:20261027T190000\r\nEND:VEVENT\r\n"
        + "END:VCALENDAR\r\n";

    private final ExternalFeed feed = new ExternalFeed(UUID.randomUUID(), UUID.randomUUID(), "École",
        URI.create("https://ecole.example/v.ics"), "event-cyan", UUID.randomUUID(), Instant.EPOCH,
        Instant.parse("2026-10-13T00:00:00Z"), Instant.parse("2026-10-13T00:00:00Z"), null, null, null);

    @Test
    void aFollowedCalendarIsExpandedOnTheWindowInTheSpacesZone() {
        List<CalendarOccurrence> found = reader.occurrencesOf(feed, () -> FEED, PARIS, Locale.FRENCH,
            LocalDate.of(2026, 10, 12), LocalDate.of(2026, 11, 1));

        assertThat(found).extracting(CalendarOccurrence::title, CalendarOccurrence::startDate).containsExactlyInAnyOrder(
            tuple("Vacances de la Toussaint", LocalDate.of(2026, 10, 17)),
            tuple("Club", LocalDate.of(2026, 10, 12)),
            tuple("Club (gymnase B)", LocalDate.of(2026, 10, 27)));
        assertThat(found).allSatisfy(occurrence -> {
            assertThat(occurrence.source()).isEqualTo(CalendarSourceType.EXTERNAL);
            assertThat(occurrence.externalFeedId()).isEqualTo(feed.id());
            assertThat(occurrence.color()).isEqualTo("event-cyan");
            assertThat(occurrence.materialized()).isTrue();
        });
        assertThat(found).filteredOn(o -> o.title().equals("Vacances de la Toussaint")).singleElement()
            .satisfies(holiday -> assertThat(holiday.endDate()).isEqualTo(LocalDate.of(2026, 11, 1)));
        assertThat(found).filteredOn(o -> o.title().equals("Club")).singleElement()
            .satisfies(club -> assertThat(club.description()).isEqualTo("Maillots"));
    }

    @Test
    void aCalendarIsParsedOnceForAsLongAsItIsTheSame() {
        AtomicInteger loads = new AtomicInteger();
        Supplier<String> content = () -> {
            loads.incrementAndGet();
            return FEED;
        };
        reader.occurrencesOf(feed, content, PARIS, Locale.FRENCH, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        reader.occurrencesOf(feed, content, PARIS, Locale.FRENCH, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
        assertThat(loads).hasValue(1);

        ExternalFeed refreshed = new ExternalFeed(feed.id(), feed.spaceId(), feed.name(), feed.url(), feed.color(),
            feed.createdBy(), feed.createdAt(), Instant.parse("2026-10-14T00:00:00Z"), Instant.parse("2026-10-14T00:00:00Z"),
            null, null, null);
        reader.occurrencesOf(refreshed, content, PARIS, Locale.FRENCH, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        assertThat(loads).as("a calendar downloaded again is read again").hasValue(2);
    }

    @Test
    void anOccurrenceThatBeganBeforeTheWindowAndRunsIntoItIsShown() {
        List<CalendarOccurrence> found = reader.occurrencesOf(feed, () -> FEED, PARIS, Locale.FRENCH,
            LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 25));

        assertThat(found).extracting(CalendarOccurrence::title).containsExactly("Vacances de la Toussaint");
    }

    @Test
    void aHostileRuleCostsItsEventAndNothingElse() {
        String hostile = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:x\r\n"
            + "BEGIN:VEVENT\r\nUID:loop\r\nDTSTAMP:20260901T000000Z\r\nSUMMARY:Boucle\r\n"
            + "DTSTART:19900101T000000Z\r\nDTEND:19900101T000001Z\r\nRRULE:FREQ=SECONDLY\r\nEND:VEVENT\r\n"
            + "BEGIN:VEVENT\r\nUID:fine\r\nDTSTAMP:20260901T000000Z\r\nSUMMARY:Normal\r\n"
            + "DTSTART;VALUE=DATE:20261014\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n";

        List<CalendarOccurrence> found = assertTimeoutPreemptively(Duration.ofSeconds(2), () -> reader.occurrencesOf(
            feed, () -> hostile, PARIS, Locale.FRENCH, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)));

        assertThat(found).extracting(CalendarOccurrence::title).containsExactly("Normal");
    }

    @Test
    void aCancelledEventIsNotShown() {
        String cancelled = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:x\r\n"
            + "BEGIN:VEVENT\r\nUID:a\r\nDTSTAMP:20260901T000000Z\r\nSUMMARY:Annulé\r\nSTATUS:CANCELLED\r\n"
            + "DTSTART;VALUE=DATE:20261014\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n";

        assertThat(reader.occurrencesOf(feed, () -> cancelled, PARIS, Locale.FRENCH,
            LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))).isEmpty();
    }

    @Test
    void aCalendarWithManyForeignZonesIsWeighedByThem() {
        // Each unknown VTIMEZONE holds an id of ical4j's global pool while the calendar is cached: its weight says so.
        StringBuilder zones = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            zones.append("BEGIN:VTIMEZONE\r\nTZID:Made Up ").append(i)
                .append("\r\nBEGIN:STANDARD\r\nDTSTART:19700101T000000\r\nTZOFFSETFROM:+0100\r\nTZOFFSETTO:+0100\r\nEND:STANDARD\r\nEND:VTIMEZONE\r\n");
        }
        String content = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:x\r\n" + zones + "END:VCALENDAR\r\n";

        assertThat(Ical4jFeedReaderAdapter.weightOf(content, IcalSupport.parse(content)))
            .isBetween(content.length() + 3 * Ical4jFeedReaderAdapter.ZONE_WEIGHT,
                content.length() + 3 * Ical4jFeedReaderAdapter.ZONE_WEIGHT + 20 * Ical4jFeedReaderAdapter.PROPERTY_WEIGHT);
    }

    @Test
    void aCalendarOfManyTinyPropertiesWeighsManyTimesItsText() {
        // Measured on 5 MB of file: 39 MB of heap for a realistic calendar, 142 MB for lines of "X-A:1" — not 5 MB.
        String content = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:x\r\n" + "X-A:1\r\n".repeat(10_000) + "END:VCALENDAR\r\n";

        assertThat(Ical4jFeedReaderAdapter.weightOf(content, IcalSupport.parse(content)))
            .isGreaterThan(10 * content.length());
    }

    private static String vevent(String uid, String summary, String extra) {
        return "BEGIN:VEVENT\r\nUID:" + uid + "\r\nDTSTAMP:20260901T000000Z\r\nSUMMARY:" + summary + "\r\n" + extra + "END:VEVENT\r\n";
    }

    private static String calendar(String... events) {
        return "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:x\r\n" + String.join("", events) + "END:VCALENDAR\r\n";
    }

    private ExternalFeed anotherFeed() {
        return new ExternalFeed(UUID.randomUUID(), UUID.randomUUID(), "Autre", URI.create("https://autre.example/v.ics"),
            "event-cyan", UUID.randomUUID(), Instant.EPOCH, Instant.parse("2026-10-13T00:00:00Z"),
            Instant.parse("2026-10-13T00:00:00Z"), null, null, null);
    }

    @Test
    void anOccurrenceIdNeverCarriesTheUid() {
        String huge = calendar(vevent("U".repeat(100_000), "Long UID",
            "DTSTART:20261001T000000Z\r\nDTEND:20261001T003000Z\r\nRRULE:FREQ=HOURLY\r\n"));

        long started = System.nanoTime();
        List<CalendarOccurrence> found = reader.occurrencesOf(anotherFeed(), () -> huge, PARIS, Locale.FRENCH,
            LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7));

        assertThat(found).hasSizeGreaterThan(100);
        assertThat(found).allSatisfy(o -> assertThat(o.sourceId()).hasSizeLessThan(100));
        assertThat(found).extracting(CalendarOccurrence::sourceId).doesNotHaveDuplicates();
        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(2));
    }

    @Test
    void anOccurrenceIdIsTheSameFromOneReadToTheNext() {
        ExternalFeed one = anotherFeed();
        List<String> first = reader.occurrencesOf(one, () -> FEED, PARIS, Locale.FRENCH, LocalDate.of(2026, 10, 12),
            LocalDate.of(2026, 11, 1)).stream().map(CalendarOccurrence::sourceId).toList();
        List<String> second = reader.occurrencesOf(one, () -> FEED, PARIS, Locale.FRENCH, LocalDate.of(2026, 10, 12),
            LocalDate.of(2026, 11, 1)).stream().map(CalendarOccurrence::sourceId).toList();

        assertThat(first).isNotEmpty().isEqualTo(second);
    }

    /** About 540 000 of a budget of one million, in the cost model of {@code BoundedExpansion}. */
    private static String heavy(String uid, String title) {
        String minutes = java.util.stream.IntStream.range(0, 60).mapToObj(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        String seconds = java.util.stream.IntStream.range(0, 12).mapToObj(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        return vevent(uid, title, "DTSTART:20260101T000000Z\r\nDTEND:20260101T000000Z\r\nRRULE:FREQ=HOURLY;BYMINUTE="
            + minutes + ";BYSECOND=" + seconds + "\r\n");
    }

    private static String normal(String uid, String title) {
        return vevent(uid, title, "DTSTART;VALUE=DATE:20261014\r\n");
    }

    @Test
    void severalCalendarsOfOneRequestShareOneBudget() {
        ExternalFeed first = anotherFeed();
        ExternalFeed second = anotherFeed();
        Map<UUID, String> files = Map.of(
            first.id(), calendar(normal("n1", "Normal 1"), heavy("h1", "Heavy 1")),
            second.id(), calendar(normal("n2", "Normal 2"), heavy("h2", "Heavy 2")));

        List<CalendarOccurrence> found = assertTimeoutPreemptively(Duration.ofSeconds(1), () -> reader.occurrencesOf(
            List.of(first, second), feed -> files.get(feed.id()), PARIS, Locale.FRENCH,
            LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)));

        assertThat(found).extracting(CalendarOccurrence::title).contains("Heavy 1", "Normal 1", "Normal 2").doesNotContain("Heavy 2");
    }

    @Test
    void aRuleRefusedOnceIsNotExpandedAgain() {
        ExternalFeed one = anotherFeed();
        String file = calendar(vevent("s", "Seconds", "DTSTART:20260101T000000Z\r\nRRULE:FREQ=SECONDLY\r\n"), normal("n", "Normal"));
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);

        reader.occurrencesOf(one, () -> file, PARIS, Locale.FRENCH, from, to);
        assertThat(reader.expansionsAttempted()).as("the rule and the plain event are tried").isEqualTo(2);
        List<CalendarOccurrence> again = reader.occurrencesOf(one, () -> file, PARIS, Locale.FRENCH, from, to);

        assertThat(reader.expansionsAttempted()).as("only the plain event is tried the second time").isEqualTo(3);
        assertThat(again).extracting(CalendarOccurrence::title).containsExactly("Normal");
    }

    @Test
    void anOccurrenceThatBeganBeforeTheWindowOfARecurringEventRunsIntoIt() {
        // 100 days long, every 200 days: the one of 1 July runs until 9 October and is in the window of 1 to 7 October.
        String file = calendar(vevent("long", "Long", "DTSTART;VALUE=DATE:20260701\r\nDTEND;VALUE=DATE:20261009\r\n"
            + "RRULE:FREQ=DAILY;INTERVAL=200\r\n"));

        List<CalendarOccurrence> found = reader.occurrencesOf(anotherFeed(), () -> file, PARIS, Locale.FRENCH,
            LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7));

        assertThat(found).extracting(CalendarOccurrence::startDate).containsExactly(LocalDate.of(2026, 7, 1));
    }

    @Test
    void aCalendarGivesNoMoreThanTwoThousandOccurrences() {
        String file = calendar(vevent("daily", "Daily", "DTSTART;VALUE=DATE:20200101\r\nRRULE:FREQ=DAILY\r\n"));

        assertThat(reader.occurrencesOf(anotherFeed(), () -> file, PARIS, Locale.FRENCH,
            LocalDate.of(2020, 1, 1), LocalDate.of(2026, 12, 31))).hasSize(Ical4jFeedReaderAdapter.MAX_OCCURRENCES);
    }

    @Test
    void anUnreadableCalendarIsLoadedOnlyOnce() {
        ExternalFeed one = anotherFeed();
        AtomicInteger loads = new AtomicInteger();
        Supplier<String> broken = () -> {
            loads.incrementAndGet();
            return "<html>no</html>";
        };
        LocalDate day = LocalDate.of(2026, 10, 1);

        assertThat(reader.occurrencesOf(one, broken, PARIS, Locale.FRENCH, day, day)).isEmpty();
        assertThat(reader.occurrencesOf(one, broken, PARIS, Locale.FRENCH, day, day)).isEmpty();

        assertThat(loads).hasValue(1);
    }

    @Test
    void aSecondMasterOfOneUidIsLeftOut() {
        String file = calendar(normal("same", "First"), normal("same", "Second"));

        assertThat(reader.occurrencesOf(anotherFeed(), () -> file, PARIS, Locale.FRENCH,
            LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))).extracting(CalendarOccurrence::title).containsExactly("First");
    }

    @Test
    void theLeadOfALongEventIsAppliedOnce() {
        // Hourly, each lasting 100 days: asked 200 days early, the cap is spent before the window and nothing is shown.
        String file = calendar(vevent("hourly", "Hourly", "DTSTART:20260101T000000Z\r\nDTEND:20260411T000000Z\r\nRRULE:FREQ=HOURLY\r\n"));

        assertThat(reader.occurrencesOf(anotherFeed(), () -> file, PARIS, Locale.FRENCH,
            LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7))).isNotEmpty();
    }
}
