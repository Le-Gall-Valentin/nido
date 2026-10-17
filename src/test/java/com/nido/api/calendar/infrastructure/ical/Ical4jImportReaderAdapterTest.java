package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.ImportGroup;
import com.nido.api.calendar.domain.model.ImportNote;
import com.nido.api.calendar.domain.model.ImportNoteKind;
import com.nido.api.calendar.domain.model.ImportReading;
import com.nido.api.calendar.domain.model.ImportedEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Ical4jImportReaderAdapterTest {

    static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    static final ZoneId TORONTO = ZoneId.of("America/Toronto");
    static final LocalDate TODAY = LocalDate.of(2026, 10, 13);

    final Ical4jImportReaderAdapter reader = new Ical4jImportReaderAdapter();

    static String ics(String... events) {
        return "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//test//EN\r\n" + String.join("", events) + "END:VCALENDAR\r\n";
    }

    static String vevent(String uid, String... lines) {
        return "BEGIN:VEVENT\r\nUID:" + uid + "\r\nDTSTAMP:20261001T000000Z\r\n" + String.join("\r\n", lines) + "\r\nEND:VEVENT\r\n";
    }

    ImportReading read(String content, ZoneId zone) {
        return reader.read(content, zone, TODAY, Locale.FRENCH);
    }

    static ImportedEvent onlyEvent(ImportReading reading) {
        assertThat(reading.groups()).hasSize(1);
        assertThat(reading.groups().getFirst().events()).hasSize(1);
        return reading.groups().getFirst().events().getFirst();
    }

    @Test
    void anAllDayEventEndsTheDayBeforeItsExclusiveEnd() {
        ImportedEvent event = onlyEvent(read(ics(vevent("a@test", "SUMMARY:Vacances",
            "DTSTART;VALUE=DATE:20261019", "DTEND;VALUE=DATE:20261102")), PARIS));
        assertThat(event.allDay()).isTrue();
        assertThat(event.startDate()).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(event.endDate()).isEqualTo(LocalDate.of(2026, 11, 1));
    }

    @Test
    void aGoogleEventInUtcIsPlacedInTheSpacesZoneAndItsHtmlRead() {
        ImportReading reading = read(ics(vevent("7kukuq@google.com", "SUMMARY:Entraînement",
            "DTSTART:20261014T160000Z", "DTEND:20261014T170000Z", "LOCATION:Gymnase",
            "DESCRIPTION:<b>Apporter</b> les maillots<br>Rendez-vous au <a href=\"https://club.example/plan\">gymnase</a> &amp; parking")),
            PARIS);
        ImportedEvent event = onlyEvent(reading);
        assertThat(event.startTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(event.endTime()).isEqualTo(LocalTime.of(19, 0));
        assertThat(event.description()).isEqualTo("Apporter les maillots\nRendez-vous au gymnase (https://club.example/plan) & parking");
        assertThat(reading.notes()).as("UTC is not a zone of its own").isEmpty();
    }

    @Test
    void aTimeInAnotherZoneIsConvertedAndSaidSo() {
        ImportReading reading = read(ics(vevent("b@test", "SUMMARY:Réunion",
            "DTSTART;TZID=Europe/Paris:20261014T100000", "DTEND;TZID=Europe/Paris:20261014T110000")), TORONTO);
        assertThat(onlyEvent(reading).startTime()).isEqualTo(LocalTime.of(4, 0));
        assertThat(reading.notes()).containsExactly(new ImportNote(ImportNoteKind.CONVERTED_ZONE, "Réunion", "Europe/Paris"));
    }

    @Test
    void anOutlookWindowsZoneDefinedInTheFileIsHonoured() {
        String outlook = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:Microsoft Exchange Server 2010\r\n"
            + "BEGIN:VTIMEZONE\r\nTZID:Romance Standard Time\r\n"
            + "BEGIN:STANDARD\r\nDTSTART:16010101T030000\r\nTZOFFSETFROM:+0200\r\nTZOFFSETTO:+0100\r\n"
            + "RRULE:FREQ=YEARLY;INTERVAL=1;BYDAY=-1SU;BYMONTH=10\r\nEND:STANDARD\r\n"
            + "BEGIN:DAYLIGHT\r\nDTSTART:16010101T020000\r\nTZOFFSETFROM:+0100\r\nTZOFFSETTO:+0200\r\n"
            + "RRULE:FREQ=YEARLY;INTERVAL=1;BYDAY=-1SU;BYMONTH=3\r\nEND:DAYLIGHT\r\nEND:VTIMEZONE\r\n"
            + vevent("o1", "SUMMARY:Point d'équipe", "DTSTART;TZID=Romance Standard Time:20260706T100000",
                "DTEND;TZID=Romance Standard Time:20260706T110000")
            + "END:VCALENDAR\r\n";
        assertThat(onlyEvent(read(outlook, TORONTO)).startTime()).isEqualTo(LocalTime.of(4, 0));
    }

    @Test
    void aFloatingTimeIsTakenAsItIsInTheSpacesZone() {
        ImportReading reading = read(ics(vevent("c@test", "SUMMARY:Marché", "DTSTART:20261017T090000", "DURATION:PT90M")), TORONTO);
        ImportedEvent event = onlyEvent(reading);
        assertThat(event.startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(event.endTime()).isEqualTo(LocalTime.of(10, 30));
        assertThat(reading.notes()).isEmpty();
    }

    @Test
    void aZoneNobodyKnowsIsTakenAsTheSpacesAndSaidSo() {
        ImportReading reading = read(ics(vevent("d@test", "SUMMARY:Ailleurs", "DTSTART;TZID=Foo/Bar:20261017T090000",
            "DTEND;TZID=Foo/Bar:20261017T100000")), PARIS);
        assertThat(onlyEvent(reading).startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(reading.notes()).containsExactly(new ImportNote(ImportNoteKind.UNKNOWN_ZONE, "Ailleurs", "Foo/Bar"));
    }

    @Test
    void anEventWithNoEndLastsADayOrNoTimeAtAll() {
        assertThat(onlyEvent(read(ics(vevent("e@test", "SUMMARY:Jour", "DTSTART;VALUE=DATE:20261017")), PARIS)).endDate())
            .isEqualTo(LocalDate.of(2026, 10, 17));
        assertThat(onlyEvent(read(ics(vevent("f@test", "SUMMARY:Instant", "DTSTART:20261017T090000")), PARIS)).endTime())
            .isEqualTo(LocalTime.of(9, 0));
    }

    @Test
    void anUntitledEventIsNamedAndALongTitleCut() {
        ImportReading untitled = read(ics(vevent("g@test", "DTSTART;VALUE=DATE:20261017")), PARIS);
        assertThat(onlyEvent(untitled).title()).isEqualTo("(Sans titre)");

        ImportReading long_ = read(ics(vevent("h@test", "SUMMARY:" + "x".repeat(250), "DTSTART;VALUE=DATE:20261017")), PARIS);
        assertThat(onlyEvent(long_).title()).hasSize(200);
        assertThat(long_.notes()).extracting(ImportNote::kind, ImportNote::detail)
            .containsExactly(org.assertj.core.groups.Tuple.tuple(ImportNoteKind.TRUNCATED_TEXT, "title"));
    }

    @Test
    void whatNidoDoesNotKeepIsIgnoredAndCounted() {
        ImportReading reading = read(ics(
            "BEGIN:VTODO\r\nUID:t1\r\nSUMMARY:Une tâche\r\nEND:VTODO\r\n",
            vevent("i@test", "SUMMARY:Sans date"),
            vevent("j@test", "SUMMARY:Annulé", "STATUS:CANCELLED", "DTSTART;VALUE=DATE:20261017"),
            vevent("k@test", "SUMMARY:Trop long", "DTSTART;VALUE=DATE:20260101", "DTEND;VALUE=DATE:20270201"),
            vevent("l@test", "SUMMARY:Gardé", "DTSTART;VALUE=DATE:20261017")), PARIS);

        assertThat(reading.groups()).extracting(ImportGroup::uid).containsExactly("l@test");
        assertThat(reading.ignored()).isEqualTo(4);
        assertThat(reading.notes()).extracting(ImportNote::kind, ImportNote::title, ImportNote::detail).containsExactlyInAnyOrder(
            org.assertj.core.groups.Tuple.tuple(ImportNoteKind.IGNORED, "Une tâche", "VTODO"),
            org.assertj.core.groups.Tuple.tuple(ImportNoteKind.IGNORED, "Sans date", "NO_START"),
            org.assertj.core.groups.Tuple.tuple(ImportNoteKind.IGNORED, "Annulé", "CANCELLED"),
            org.assertj.core.groups.Tuple.tuple(ImportNoteKind.IGNORED, "Trop long", "TOO_LONG"));
    }

    @Test
    void eventsAreGroupedByUidAndThoseWithoutOneStandAlone() {
        String noUid = "BEGIN:VEVENT\r\nDTSTAMP:20261001T000000Z\r\nSUMMARY:Sans UID\r\nDTSTART;VALUE=DATE:20261017\r\nEND:VEVENT\r\n";
        ImportReading reading = read(ics(noUid, noUid, vevent("m@test", "SUMMARY:Un", "DTSTART;VALUE=DATE:20261018")), PARIS);
        assertThat(reading.groups()).extracting(ImportGroup::uid).containsExactly(null, null, "m@test");
    }

    @Test
    void aFileThatIsNotACalendarIsRefused() {
        assertThatThrownBy(() -> read("<html><body>Not found</body></html>", PARIS)).isInstanceOf(CalendarException.NotAnIcalFile.class);
    }

    @Test
    void moreThanFiveThousandEventsIsTooMuch() {
        StringBuilder many = new StringBuilder();
        for (int i = 0; i <= 5_000; i++) {
            many.append(vevent("n" + i + "@test", "SUMMARY:x", "DTSTART;VALUE=DATE:20261017"));
        }
        assertThatThrownBy(() -> read(ics(many.toString()), PARIS)).isInstanceOf(CalendarException.ImportTooLarge.class);
    }
}
