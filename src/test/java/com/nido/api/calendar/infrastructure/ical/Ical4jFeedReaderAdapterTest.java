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
            .isEqualTo(content.length() + 3 * Ical4jFeedReaderAdapter.ZONE_WEIGHT);
    }
}
