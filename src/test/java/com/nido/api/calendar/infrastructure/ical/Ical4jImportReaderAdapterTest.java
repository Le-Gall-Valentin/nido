package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.ImportGroup;
import com.nido.api.calendar.domain.model.ImportNote;
import com.nido.api.calendar.domain.model.ImportNoteKind;
import com.nido.api.calendar.domain.model.ImportReading;
import com.nido.api.calendar.domain.model.ImportedEvent;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

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

    /** How Outlook writes the zone of Paris. */
    static final String OUTLOOK_ROMANCE = "BEGIN:VTIMEZONE\r\nTZID:Romance Standard Time\r\n"
        + "BEGIN:STANDARD\r\nDTSTART:16010101T030000\r\nTZOFFSETFROM:+0200\r\nTZOFFSETTO:+0100\r\n"
        + "RRULE:FREQ=YEARLY;INTERVAL=1;BYDAY=-1SU;BYMONTH=10\r\nEND:STANDARD\r\n"
        + "BEGIN:DAYLIGHT\r\nDTSTART:16010101T020000\r\nTZOFFSETFROM:+0100\r\nTZOFFSETTO:+0200\r\n"
        + "RRULE:FREQ=YEARLY;INTERVAL=1;BYDAY=-1SU;BYMONTH=3\r\nEND:DAYLIGHT\r\nEND:VTIMEZONE\r\n";

    /** How Outlook writes the zone of New York: summer time from the second Sunday of March. */
    static final String OUTLOOK_EASTERN = "BEGIN:VTIMEZONE\r\nTZID:Eastern Standard Time\r\n"
        + "BEGIN:STANDARD\r\nDTSTART:16010101T020000\r\nTZOFFSETFROM:-0400\r\nTZOFFSETTO:-0500\r\n"
        + "RRULE:FREQ=YEARLY;INTERVAL=1;BYDAY=1SU;BYMONTH=11\r\nEND:STANDARD\r\n"
        + "BEGIN:DAYLIGHT\r\nDTSTART:16010101T020000\r\nTZOFFSETFROM:-0500\r\nTZOFFSETTO:-0400\r\n"
        + "RRULE:FREQ=YEARLY;INTERVAL=1;BYDAY=2SU;BYMONTH=3\r\nEND:DAYLIGHT\r\nEND:VTIMEZONE\r\n";

    @Test
    void anOutlookWindowsZoneDefinedInTheFileIsHonoured() {
        String outlook = ics(OUTLOOK_ROMANCE, vevent("o1", "SUMMARY:Point d'équipe",
            "DTSTART;TZID=Romance Standard Time:20260706T100000", "DTEND;TZID=Romance Standard Time:20260706T110000"));
        assertThat(onlyEvent(read(outlook, TORONTO)).startTime()).isEqualTo(LocalTime.of(4, 0));
    }

    @Test
    void aFileWhoseOwnZoneWouldChangeEveryHourIsRefusedAtOnce() {
        String hourly = "BEGIN:VTIMEZONE\r\nTZID:Bureau\r\nBEGIN:STANDARD\r\nDTSTART:16010101T000000\r\nTZOFFSETFROM:+0100\r\n"
            + "TZOFFSETTO:+0000\r\nRRULE:FREQ=HOURLY\r\nEND:STANDARD\r\nEND:VTIMEZONE\r\n";
        String content = ics(hourly, vevent("b1", "SUMMARY:x", "DTSTART;TZID=Bureau:20261017T090000"));

        assertThatThrownBy(() -> assertTimeoutPreemptively(Duration.ofSeconds(10), () -> read(content, PARIS)))
            .isInstanceOf(CalendarException.NotAnIcalFile.class);
    }

    @Test
    void anOutlookZoneThatIsTheSpacesIsNoConversion() {
        ImportReading reading = read(ics(OUTLOOK_ROMANCE, vevent("o1", "SUMMARY:Point d'équipe",
            "DTSTART;TZID=Romance Standard Time:20270310T100000", "DTEND;TZID=Romance Standard Time:20270310T110000")), PARIS);
        assertThat(onlyEvent(reading).startTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(reading.notes()).isEmpty();
    }

    @Test
    void anOutlookUsZoneMovesToSummerTimeOnTheSecondSundayOfMarch() {
        // New York moves on 14 March 2027 and Paris on the 28th: 9:00 there is 15:00 here on the 10th, 14:00 on the 17th.
        ImportReading reading = read(ics(OUTLOOK_EASTERN,
            vevent("o1", "SUMMARY:Avant", "DTSTART;TZID=Eastern Standard Time:20270310T090000", "DURATION:PT1H"),
            vevent("o2", "SUMMARY:Après", "DTSTART;TZID=Eastern Standard Time:20270317T090000", "DURATION:PT1H")), PARIS);
        assertThat(reading.groups()).extracting(group -> group.events().getFirst())
            .extracting(ImportedEvent::title, ImportedEvent::startTime)
            .containsExactlyInAnyOrder(tuple("Avant", LocalTime.of(15, 0)), tuple("Après", LocalTime.of(14, 0)));
    }

    @Test
    void aWeeklyOutlookSeriesInAUsZoneFollowsItsSummerTime() {
        ImportReading reading = read(ics(OUTLOOK_EASTERN, vevent("o3", "SUMMARY:Point US",
            "DTSTART;TZID=Eastern Standard Time:20270303T090000", "DTEND;TZID=Eastern Standard Time:20270303T100000",
            "RRULE:FREQ=WEEKLY;COUNT=3")), PARIS);
        assertThat(reading.groups().getFirst().events()).extracting(event -> LocalDateTime.of(event.startDate(), event.startTime()))
            .containsExactly(LocalDateTime.of(2027, 3, 3, 15, 0), LocalDateTime.of(2027, 3, 10, 15, 0),
                LocalDateTime.of(2027, 3, 17, 14, 0));
    }

    @Test
    void aWindowsNameIcal4jTakesForAnotherZoneKeepsTheFilesOwnDefinition() {
        // ical4j's table says "Samoa Standard Time" is Pago Pago (UTC-11); Outlook means Apia (UTC+13), a day away.
        String samoa = "BEGIN:VTIMEZONE\r\nTZID:Samoa Standard Time\r\nBEGIN:STANDARD\r\nDTSTART:16010101T000000\r\n"
            + "TZOFFSETFROM:+1300\r\nTZOFFSETTO:+1300\r\nEND:STANDARD\r\nEND:VTIMEZONE\r\n";
        ImportedEvent event = onlyEvent(read(ics(samoa, vevent("s1", "SUMMARY:Apia",
            "DTSTART;TZID=Samoa Standard Time:20270115T090000", "DURATION:PT1H")), PARIS));
        assertThat(LocalDateTime.of(event.startDate(), event.startTime())).isEqualTo(LocalDateTime.of(2027, 1, 14, 21, 0));
    }

    @Test
    void aWindowsNameWithoutItsDefinitionIsReadAsTheZoneItStandsFor() {
        ImportReading reading = read(ics(vevent("w1", "SUMMARY:Point",
            "DTSTART;TZID=Romance Standard Time:20270310T100000", "DURATION:PT1H")), PARIS);
        assertThat(onlyEvent(reading).startTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(reading.notes()).isEmpty();
    }

    @Test
    void aZoneNamedUnderAGlobalPrefixIsReadByTheNameAfterIt() {
        // Lightning named its zones /mozilla.org/<version>/<zone>, and files written then still carry those names.
        String lightning = "/mozilla.org/20070129_1/America/New_York";
        ImportReading reading = read(ics(vevent("l1", "SUMMARY:Réunion", "DTSTART;TZID=" + lightning + ":20270310T090000",
            "DURATION:PT1H")), PARIS);
        assertThat(onlyEvent(reading).startTime()).isEqualTo(LocalTime.of(15, 0));
        assertThat(reading.notes()).containsExactly(new ImportNote(ImportNoteKind.CONVERTED_ZONE, "Réunion", lightning));
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

    @Test
    void aTextOverTheRawCapIsCutOnceAndSaidSo() {
        ImportReading reading = read(ics(vevent("p@test", "SUMMARY:Long", "DTSTART;VALUE=DATE:20261017",
            "DESCRIPTION:" + "d".repeat(20_000))), PARIS);
        assertThat(onlyEvent(reading).description()).hasSize(2000);
        assertThat(reading.notes()).extracting(ImportNote::kind, ImportNote::detail)
            .containsExactly(org.assertj.core.groups.Tuple.tuple(ImportNoteKind.TRUNCATED_TEXT, "description"));
    }

    @Test
    void aControlCharacterInATextIsRemoved() {
        ImportedEvent event = onlyEvent(read(ics(vevent("q@test", "SUMMARY:A\u0000B\u0007C", "DTSTART;VALUE=DATE:20261017")), PARIS));
        assertThat(event.title()).isEqualTo("ABC");
    }

    @Test
    void aCutNeverSplitsASurrogatePair() {
        ImportedEvent event = onlyEvent(read(ics(vevent("r@test", "SUMMARY:" + "x".repeat(199) + "\uD83D\uDE00",
            "DTSTART;VALUE=DATE:20261017")), PARIS));
        assertThat(event.title()).isEqualTo("x".repeat(199));
    }

    private static ImportReading tooLong(ImportReading reading) {
        assertThat(reading.groups()).isEmpty();
        assertThat(reading.ignored()).isEqualTo(1);
        assertThat(reading.notes()).extracting(ImportNote::kind, ImportNote::detail)
            .containsExactly(org.assertj.core.groups.Tuple.tuple(ImportNoteKind.IGNORED, "TOO_LONG"));
        return reading;
    }

    @Test
    void anAbsurdDurationIsAnEventTooLongToHold() {
        tooLong(read(ics(vevent("s@test", "SUMMARY:Absurde", "DTSTART:20261017T090000", "DURATION:P999999999D")), PARIS));
    }

    @Test
    void anEventRunningPastYear9999IsTooLongToHold() {
        tooLong(read(ics(vevent("u@test", "SUMMARY:Fin", "DTSTART:99991231T230000", "DURATION:PT2H")), PARIS));
    }

    @Test
    void aNegativeDurationEndsWhereTheEventStarts() {
        ImportedEvent event = onlyEvent(read(ics(vevent("v@test", "SUMMARY:Retour", "DTSTART:20261017T090000", "DURATION:-PT1H")), PARIS));
        assertThat(event.startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(event.endTime()).isEqualTo(LocalTime.of(9, 0));
    }

    @Test
    void startsInYear0AndYear9999AreReadWithoutThrowing() {
        for (String start : new String[] {"00000101T100000", "99991231T100000", "99991231T235959Z"}) {
            // ical4j refuses year 0 outright: that event is then set aside as unreadable, which is an answer, not a crash.
            ImportReading reading = read(ics(vevent("w" + start, "SUMMARY:Limite", "DTSTART:" + start)), PARIS);
            assertThat(reading.groups().size() + reading.ignored()).isEqualTo(1);
        }
    }

    @Test
    void aSecondMasterWithTheSameUidIsSetAsideAndSaidSo() {
        ImportReading reading = read(ics(vevent("x@test", "SUMMARY:Premier", "DTSTART;VALUE=DATE:20261017"),
            vevent("x@test", "SUMMARY:Second", "DTSTART;VALUE=DATE:20261018")), PARIS);
        assertThat(reading.groups()).hasSize(1);
        assertThat(reading.ignored()).isEqualTo(1);
        assertThat(reading.notes()).containsExactly(new ImportNote(ImportNoteKind.IGNORED, "Second", "DUPLICATE_UID"));
    }

    @Test
    void anEditedOccurrenceWithoutItsSeriesIsReadLikeAnyEventAndSaidWhenSetAside() {
        ImportReading reading = read(ics(
            vevent("y@test", "SUMMARY:Orpheline", "RECURRENCE-ID;TZID=Europe/Paris:20261014T100000",
                "DTSTART;TZID=Europe/Paris:20261014T110000", "DTEND;TZID=Europe/Paris:20261014T120000"),
            vevent("z@test", "SUMMARY:Annulée", "RECURRENCE-ID;VALUE=DATE:20261014", "STATUS:CANCELLED",
                "DTSTART;VALUE=DATE:20261014")), TORONTO);
        assertThat(reading.groups()).extracting(ImportGroup::uid).containsExactly("y@test");
        assertThat(reading.ignored()).isEqualTo(1);
        assertThat(reading.notes()).containsExactlyInAnyOrder(
            new ImportNote(ImportNoteKind.CONVERTED_ZONE, "Orpheline", "Europe/Paris"),
            new ImportNote(ImportNoteKind.IGNORED, "Annulée", "CANCELLED"));
    }

    @Test
    void aZoneKnownUnderAnotherNameOfTheSpacesIsNotAConversion() {
        ImportReading reading = read(ics(vevent("aa@test", "SUMMARY:Alias", "DTSTART;TZID=Asia/Calcutta:20261017T090000",
            "DTEND;TZID=Asia/Calcutta:20261017T100000")), ZoneId.of("Asia/Kolkata"));
        assertThat(onlyEvent(reading).startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(reading.notes()).isEmpty();
    }

    @Test
    void anOverrideOfTheStartOfAnEventThatDoesNotRepeatReplacesIt() {
        ImportReading reading = read(ics(vevent("y@test", "SUMMARY:Avant", "DTSTART;TZID=Europe/Paris:20261017T090000"),
            vevent("y@test", "SUMMARY:Après", "RECURRENCE-ID;TZID=Europe/Paris:20261017T090000",
                "DTSTART;TZID=Europe/Paris:20261017T110000")), PARIS);
        ImportedEvent event = onlyEvent(reading);
        assertThat(event.title()).isEqualTo("Après");
        assertThat(event.startTime()).isEqualTo(LocalTime.of(11, 0));
        assertThat(reading.ignored()).isZero();
    }

    @Test
    void anyOtherOverrideOfAnEventThatDoesNotRepeatIsAnEventOfItsOwn() {
        ImportReading reading = read(ics(vevent("z@test", "SUMMARY:Seul", "DTSTART;TZID=Europe/Paris:20261017T090000"),
            vevent("z@test", "SUMMARY:Ailleurs", "RECURRENCE-ID;TZID=Europe/Paris:20261024T090000",
                "DTSTART;TZID=Europe/Paris:20261024T100000")), PARIS);
        assertThat(reading.groups()).hasSize(1);
        assertThat(reading.groups().getFirst().events()).extracting(ImportedEvent::title).containsExactly("Seul", "Ailleurs");
    }

    @Test
    void aCancelledOverrideOfTheStartOfAnEventThatDoesNotRepeatRemovesIt() {
        ImportReading reading = read(ics(vevent("c@test", "SUMMARY:Annulé", "DTSTART;TZID=Europe/Paris:20261017T090000"),
            vevent("c@test", "SUMMARY:Annulé", "STATUS:CANCELLED", "RECURRENCE-ID;TZID=Europe/Paris:20261017T090000",
                "DTSTART;TZID=Europe/Paris:20261017T090000")), PARIS);
        assertThat(reading.groups()).isEmpty();
        assertThat(reading.ignored()).isEqualTo(1);
        assertThat(reading.notes()).containsExactly(new ImportNote(ImportNoteKind.IGNORED, "Annulé", "CANCELLED"));
    }

    @Test
    void anItemOfAnotherPartOfNidoIsNeverImportedAndSaidSo() {
        // What a nido export writes for a task, a meal, a debit and a goal: a copy frozen in the calendar is never wanted.
        ImportReading reading = read(ics(
            vevent("TASK-5d0f6a0e-8f7c-4b8e-9a51-2f0c2b8a1d11@nido", "SUMMARY:Tâche · Poubelles", "DTSTART;VALUE=DATE:20261017"),
            vevent("MEAL-7b1e2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d:2026-10-17@nido", "SUMMARY:Repas · Lasagnes", "DTSTART;VALUE=DATE:20261017"),
            vevent("FINANCE-1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d:2026-10-17@nido", "SUMMARY:Finance · Loyer", "DTSTART;VALUE=DATE:20261017"),
            vevent("SAVINGS-9f8e7d6c-5b4a-4321-8fed-cba987654321@nido", "SUMMARY:Épargne · Vélo", "DTSTART;VALUE=DATE:20261017"),
            vevent("TASK-42@example.com", "SUMMARY:Ailleurs", "DTSTART;VALUE=DATE:20261017")), PARIS);
        assertThat(reading.groups()).extracting(ImportGroup::uid).containsExactly("TASK-42@example.com");
        assertThat(reading.ignored()).isEqualTo(4);
        assertThat(reading.notes()).containsExactly(
            new ImportNote(ImportNoteKind.IGNORED, "Tâche · Poubelles", "NIDO_SOURCE"),
            new ImportNote(ImportNoteKind.IGNORED, "Repas · Lasagnes", "NIDO_SOURCE"),
            new ImportNote(ImportNoteKind.IGNORED, "Finance · Loyer", "NIDO_SOURCE"),
            new ImportNote(ImportNoteKind.IGNORED, "Épargne · Vélo", "NIDO_SOURCE"));
    }

    @Test
    void anOverrideRepeatedForTheStartOfAnEventThatDoesNotRepeatIsReadOnce() {
        ImportReading reading = read(ics(vevent("r@test", "SUMMARY:Avant", "DTSTART;TZID=Europe/Paris:20261017T090000"),
            vevent("r@test", "SUMMARY:Après", "RECURRENCE-ID;TZID=Europe/Paris:20261017T090000",
                "DTSTART;TZID=Europe/Paris:20261017T110000"),
            vevent("r@test", "SUMMARY:Après (copie)", "RECURRENCE-ID;TZID=Europe/Paris:20261017T090000",
                "DTSTART;TZID=Europe/Paris:20261017T110000")), PARIS);
        assertThat(onlyEvent(reading).title()).isEqualTo("Après");
        assertThat(reading.ignored()).isEqualTo(1);
        assertThat(reading.notes()).containsExactly(new ImportNote(ImportNoteKind.IGNORED, "Après (copie)", "DUPLICATE_UID"));
    }

    @Test
    void aPropertyNidoNeverReadsThatDoesNotParseCostsNothing() {
        // Google writes CREATED:00001231T000000Z on some events; the others are as malformed as files get.
        for (String odd : List.of("CREATED:00001231T000000Z", "SEQUENCE:abc", "LAST-MODIFIED:2026", "GEO:abc", "DTSTAMP:hier")) {
            ImportReading reading = read(ics(vevent("g@google.com", "SUMMARY:Anniversaire", "DTSTART;VALUE=DATE:20261017", odd)), PARIS);
            assertThat(onlyEvent(reading).title()).as(odd).isEqualTo("Anniversaire");
            assertThat(reading.ignored()).as(odd).isZero();
        }
    }

    @Test
    void anEventWithAPropertyNidoReadsThatDoesNotParseIsSetAsideAndTheRestOfTheFileIsRead() {
        // What an event is, when and how it repeats, is never guessed: that event goes, with its edited occurrences.
        ImportReading reading = read(ics(
            vevent("ok@test", "SUMMARY:Lu", "DTSTART:20261017T090000"),
            vevent("d@test", "SUMMARY:Sans début lisible", "DTSTART:notadate"),
            vevent("r@test", "SUMMARY:Règle illisible", "DTSTART;VALUE=DATE:20261017", "RRULE:FREQ=WEEKLY;COUNT=x"),
            vevent("o@test", "SUMMARY:Série", "DTSTART:20261019T090000", "RRULE:FREQ=WEEKLY;COUNT=3"),
            vevent("o@test", "SUMMARY:Déplacée", "RECURRENCE-ID:pas-une-date", "DTSTART:20261027T100000")), PARIS);

        assertThat(onlyEvent(reading).title()).isEqualTo("Lu");
        assertThat(reading.ignored()).isEqualTo(3);
        assertThat(reading.notes()).extracting(ImportNote::kind, ImportNote::title, ImportNote::detail).containsExactly(
            tuple(ImportNoteKind.IGNORED, "Sans début lisible", "MALFORMED"),
            tuple(ImportNoteKind.IGNORED, "Règle illisible", "MALFORMED"),
            tuple(ImportNoteKind.IGNORED, "Série", "MALFORMED"));
    }

    @Test
    void aMalformedDueOfATaskNidoSetsAsideRefusesNothing() {
        ImportReading reading = read(ics(vevent("ok@test", "SUMMARY:Lu", "DTSTART:20261017T090000"),
            "BEGIN:VTODO\r\nUID:t@test\r\nDTSTAMP:20261001T000000Z\r\nSUMMARY:Tâche\r\nDUE:00001231T000000Z-x\r\nEND:VTODO\r\n"), PARIS);

        assertThat(onlyEvent(reading).title()).isEqualTo("Lu");
        assertThat(reading.notes()).extracting(ImportNote::detail).containsExactly("VTODO");
    }

    @Test
    void anEditedFirstDayOffItsRuleTakesItsPlaceOrCancelsIt() {
        // Mondays from Wednesday 7 January: the Wednesday is an event of its own, which its edited version replaces.
        String rule = "RRULE:FREQ=WEEKLY;BYDAY=MO;UNTIL=20260131T000000Z";
        ImportReading moved = read(ics(
            vevent("a@test", "SUMMARY:Atelier", "DTSTART:20260107T090000", rule),
            vevent("a@test", "SUMMARY:Atelier (déplacé)", "RECURRENCE-ID:20260107T090000", "DTSTART:20260108T090000")), PARIS);
        ImportReading cancelled = read(ics(
            vevent("c@test", "SUMMARY:Atelier", "DTSTART:20260107T090000", rule),
            vevent("c@test", "SUMMARY:Atelier", "RECURRENCE-ID:20260107T090000", "DTSTART:20260107T090000", "STATUS:CANCELLED")), PARIS);

        assertThat(moved.groups().getFirst().series()).isNotNull();
        assertThat(moved.groups().getFirst().events()).extracting(ImportedEvent::title, ImportedEvent::startDate)
            .containsExactly(tuple("Atelier (déplacé)", LocalDate.of(2026, 1, 8)));
        assertThat(cancelled.groups().getFirst().series()).isNotNull();
        assertThat(cancelled.groups().getFirst().events()).isEmpty();
    }

    @Test
    void anEditedFirstDayOfARuleGivingNothingElseTakesItsPlace() {
        // A rule whose UNTIL comes before its first Monday gives nothing: only the first day is left, edited on its own.
        ImportReading reading = read(ics(
            vevent("n@test", "SUMMARY:Atelier", "DTSTART:20260107T090000", "RRULE:FREQ=WEEKLY;BYDAY=MO;UNTIL=20260110T000000Z"),
            vevent("n@test", "SUMMARY:Atelier (déplacé)", "RECURRENCE-ID:20260107T090000", "DTSTART:20260108T090000")), PARIS);

        assertThat(onlyEvent(reading).title()).isEqualTo("Atelier (déplacé)");
    }

    @Test
    void anOverrideWhoseSequenceIsNoNumberCountsAsTheFirstVersion() {
        String master = vevent("s@test", "SUMMARY:Piano", "DTSTART;TZID=Europe/Paris:20261005T180000",
            "DTEND;TZID=Europe/Paris:20261005T190000", "RRULE:FREQ=WEEKLY;BYDAY=MO");
        String numbered = vevent("s@test", "SUMMARY:Piano (salle 3)", "SEQUENCE:1", "RECURRENCE-ID;TZID=Europe/Paris:20261012T180000",
            "DTSTART;TZID=Europe/Paris:20261012T190000", "DTEND;TZID=Europe/Paris:20261012T200000");
        String unreadable = vevent("s@test", "SUMMARY:Piano (salle 2)", "SEQUENCE:abc", "RECURRENCE-ID;TZID=Europe/Paris:20261012T180000",
            "DTSTART;TZID=Europe/Paris:20261012T190000", "DTEND;TZID=Europe/Paris:20261012T200000");

        for (String content : List.of(ics(master, unreadable, numbered), ics(master, numbered, unreadable))) {
            ImportReading reading = read(content, PARIS);
            assertThat(reading.groups().getFirst().series().editedOccurrences())
                .extracting(edited -> edited.event().title()).containsExactly("Piano (salle 3)");
            assertThat(reading.notes()).containsExactly(new ImportNote(ImportNoteKind.IGNORED, "Piano (salle 2)", "DUPLICATE_UID"));
        }
    }
}
