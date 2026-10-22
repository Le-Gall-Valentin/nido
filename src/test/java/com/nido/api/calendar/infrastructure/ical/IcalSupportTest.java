package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.model.CalendarException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.ZoneRulesProviderImpl;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtStart;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class IcalSupportTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private static final String NEW_YORK = """
        BEGIN:VTIMEZONE
        TZID:America/New_York
        X-LIC-LOCATION:America/New_York
        BEGIN:DAYLIGHT
        TZOFFSETFROM:-0500
        TZOFFSETTO:-0400
        TZNAME:EDT
        DTSTART:19700308T020000
        RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=2SU
        END:DAYLIGHT
        BEGIN:STANDARD
        TZOFFSETFROM:-0400
        TZOFFSETTO:-0500
        TZNAME:EST
        DTSTART:19701101T020000
        RRULE:FREQ=YEARLY;BYMONTH=11;BYDAY=1SU
        END:STANDARD
        END:VTIMEZONE
        """;

    private static String document(String zones, String event) {
        return "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//test//EN\n" + zones + "BEGIN:VEVENT\nUID:e@test\nDTSTAMP:20260101T000000Z\n"
            + event + "SUMMARY:x\nEND:VEVENT\nEND:VCALENDAR\n";
    }

    private static VEvent event(String ics) {
        return IcalSupport.parse(ics).<VEvent>getComponents(Component.VEVENT).getFirst();
    }

    @Test
    void aZoneTheJdkKnowsIsReadWithTheJdksRulesWhateverTheDocumentSays() {
        VEvent e = event(document(NEW_YORK, "DTSTART;TZID=America/New_York:20270310T090000\n"));
        assertThat(IcalSupport.toLocal(e.<DtStart<Temporal>>getProperty(Property.DTSTART).orElseThrow().getDate(), PARIS))
            .isEqualTo(LocalDateTime.of(2027, 3, 10, 15, 0));
    }

    @Test
    void aZoneTheJdkKnowsNeedsNoDefinitionInTheDocument() {
        VEvent e = event(document("", "DTSTART;TZID=America/New_York:20270310T090000\n"));
        assertThat(IcalSupport.toLocal(e.<DtStart<Temporal>>getProperty(Property.DTSTART).orElseThrow().getDate(), PARIS))
            .isEqualTo(LocalDateTime.of(2027, 3, 10, 15, 0));
    }

    @Test
    void aWeeklySeriesKeepsItsWallClockAcrossBothChanges() {
        VEvent e = event(document("", "DTSTART;TZID=America/New_York:20270224T090000\nRRULE:FREQ=WEEKLY;COUNT=4\n"));
        Set<Period<Temporal>> set = e.calculateRecurrenceSet(new Period<>(
            LocalDate.of(2027, 1, 1).atStartOfDay(PARIS), LocalDate.of(2027, 12, 31).atStartOfDay(PARIS)));
        assertThat(set.stream().map(p -> IcalSupport.toLocal(p.getStart(), PARIS)).sorted().toList())
            .containsExactly(LocalDateTime.of(2027, 2, 24, 15, 0), LocalDateTime.of(2027, 3, 3, 15, 0),
                LocalDateTime.of(2027, 3, 10, 15, 0), LocalDateTime.of(2027, 3, 17, 14, 0));
    }

    @Test
    void aDocumentsOwnZoneDoesNotReachAnotherDocument() {
        String office = "BEGIN:VTIMEZONE\nTZID:Office\nBEGIN:STANDARD\nTZOFFSETFROM:+0300\nTZOFFSETTO:+0300\nTZNAME:OFC\n"
            + "DTSTART:19700101T000000\nEND:STANDARD\nEND:VTIMEZONE\n";
        event(document(office, "DTSTART;TZID=Office:20270310T090000\n"));

        VEvent other = event(document("", "DTSTART;TZID=Office:20270310T090000\n"));
        assertThat(other.<DtStart<Temporal>>getProperty(Property.DTSTART).orElseThrow().getDate())
            .isEqualTo(LocalDateTime.of(2027, 3, 10, 9, 0));
    }

    @Test
    void aDocumentWithTooManyOwnZonesIsRefused() {
        assertThat(IcalSupport.parse(document(zones(50), "DTSTART;TZID=Zone1:20270310T090000\n"))).isNotNull();
        assertThatThrownBy(() -> IcalSupport.parse(document(zones(51), "DTSTART;TZID=Zone1:20270310T090000\n")))
            .isInstanceOf(CalendarException.NotAnIcalFile.class);
    }

    @Test
    void aZoneRepeatedInADocumentCountsOnceAndDrawsOneIdOfIcal4jsPool() {
        // ical4j draws an id of its global pool of 1500 for every zone registered, even one it already has.
        String repeated = zones(1).repeat(200);
        int before = ZoneRulesProviderImpl.getInstance().orElseThrow().getZoneIdPool().availableZoneIds();
        Calendar parsed = IcalSupport.parse(document(repeated, "DTSTART;TZID=Zone1:20270310T090000\n"));
        int after = ZoneRulesProviderImpl.getInstance().orElseThrow().getZoneIdPool().availableZoneIds();
        assertThat(parsed).isNotNull();
        assertThat(before - after).isLessThanOrEqualTo(1);
    }

    @Test
    void aZoneChangingEveryMinuteIsRefusedBeforeIcal4jListsItsChanges() {
        // The audit's file, 321 bytes: ical4j would list every minute since 1601 and fill any heap.
        String bomb = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:x\r\nBEGIN:VTIMEZONE\r\nTZID:Bureau\r\n"
            + "BEGIN:STANDARD\r\nDTSTART:16010101T000000\r\nTZOFFSETFROM:+0100\r\nTZOFFSETTO:+0000\r\nRRULE:FREQ=MINUTELY\r\n"
            + "END:STANDARD\r\nEND:VTIMEZONE\r\nBEGIN:VEVENT\r\nUID:a\r\nDTSTAMP:20260101T000000Z\r\nSUMMARY:x\r\n"
            + "DTSTART;TZID=Bureau:20261017T090000\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n";
        assertThat(bomb).hasSize(321);

        assertThatThrownBy(() -> parseInTime(bomb)).isInstanceOf(CalendarException.NotAnIcalFile.class);
    }

    @Test
    void fiftyZonesChangingEveryHourAreRefused() {
        StringBuilder zones = new StringBuilder();
        for (int i = 1; i <= 50; i++) {
            zones.append(zone("Hourly" + i, observance("STANDARD", "16010101T000000", "+0100", "+0000", "FREQ=HOURLY")));
        }

        assertThatThrownBy(() -> parseInTime(document(zones.toString(), "DTSTART;TZID=Hourly1:20261017T090000\n")))
            .isInstanceOf(CalendarException.NotAnIcalFile.class);
    }

    @Test
    void aZoneOfANameTheJdkKnowsIsWeighedAllTheSame() {
        // ical4j lists the changes of several standard observances as soon as it reads the zone, before any name is looked up.
        String paris = zone("Europe/Paris", observance("STANDARD", "16010101T000000", "+0100", "+0000", "FREQ=MINUTELY"),
            observance("STANDARD", "16010101T000000", "+0000", "+0100", "FREQ=MINUTELY"));

        assertThatThrownBy(() -> parseInTime(document(paris, "DTSTART;TZID=Europe/Paris:20261017T090000\n")))
            .isInstanceOf(CalendarException.NotAnIcalFile.class);
    }

    @Test
    void aZoneThatDoesNotChangeOnDaysOfTheYearIsRefusedHoweverLittleItChanges() {
        String monthly = zone("Monthly", observance("STANDARD", "20260101T000000", "+0100", "+0000", "FREQ=MONTHLY;COUNT=2"));

        assertThatThrownBy(() -> parseInTime(document(monthly, "DTSTART;TZID=Monthly:20261017T090000\n")))
            .isInstanceOf(CalendarException.NotAnIcalFile.class);
    }

    @Test
    void aZoneListingMoreChangesThanAnyCalendarNeedsIsRefused() {
        // Yearly, but 28 days of every month since 1601: some 140 000 changes, more than the budget of a whole document.
        String daily = zone("Daily", observance("STANDARD", "16010101T000000", "+0100", "+0000",
            "FREQ=YEARLY;BYMONTH=1,2,3,4,5,6,7,8,9,10,11,12;BYMONTHDAY=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,"
                + "21,22,23,24,25,26,27,28"));

        assertThatThrownBy(() -> parseInTime(document(daily, "DTSTART;TZID=Daily:20261017T090000\n")))
            .isInstanceOf(CalendarException.NotAnIcalFile.class);
    }

    @Test
    void fiftyOutlookZonesSince1601AreRead() {
        StringBuilder zones = new StringBuilder();
        for (int i = 1; i <= 50; i++) {
            zones.append(outlookEastern("Office " + i));
        }

        VEvent e = parseInTime(document(zones.toString(), "DTSTART;TZID=Office 1:20270310T090000\n"))
            .<VEvent>getComponents(Component.VEVENT).getFirst();
        assertThat(IcalSupport.toLocal(e.<DtStart<Temporal>>getProperty(Property.DTSTART).orElseThrow().getDate(), PARIS))
            .isEqualTo(LocalDateTime.of(2027, 3, 10, 15, 0));
    }

    @Test
    void everyZoneIcal4jShipsIsRead() {
        // The longest zones written: ical4j's own definitions, with every change since the zone began.
        List<String> shipped = ZoneId.getAvailableZoneIds().stream().sorted()
            .filter(id -> IcalSupport.registry().getTimeZone(id) != null).toList();
        assertThat(shipped).hasSizeGreaterThan(400);

        for (String id : shipped) {
            String definition = IcalSupport.registry().getTimeZone(id).getVTimeZone().toString();
            assertThat(parseInTime(document(definition, "DTSTART;TZID=" + id + ":20270310T090000\n"))).as(id).isNotNull();
        }
    }

    @Test
    void aZoneGivenAgainIsTheZoneFirstGiven() {
        // Given again, with two standard observances that ical4j would list as soon as it read them.
        String office = zone("Office", observance("STANDARD", "19700101T000000", "+0300", "+0300", null));
        String changingEveryMinute = zone("Office", observance("STANDARD", "16010101T000000", "+0100", "+0000", "FREQ=MINUTELY"),
            observance("STANDARD", "16010101T000000", "+0000", "+0100", "FREQ=MINUTELY"));

        VEvent e = parseInTime(document(office + changingEveryMinute, "DTSTART;TZID=Office:20270310T090000\n"))
            .<VEvent>getComponents(Component.VEVENT).getFirst();
        assertThat(IcalSupport.toLocal(e.<DtStart<Temporal>>getProperty(Property.DTSTART).orElseThrow().getDate(), ZoneOffset.UTC))
            .isEqualTo(LocalDateTime.of(2027, 3, 10, 6, 0));
    }

    @Test
    void aCalendarMergedFromManyInvitationsEachWithItsZoneIsRead() {
        // An Outlook zone since 1601 lists some 900 changes: two hundred copies would be weighed at their sum.
        assertThat(parseInTime(document(outlookEastern("Eastern Standard Time").repeat(200),
            "DTSTART;TZID=Eastern Standard Time:20270310T090000\n"))).isNotNull();
    }

    @Test
    void anUnreadableValueOfAPropertyNidoNeverReadsIsKeptAsWritten() {
        VEvent google = event(document("", "DTSTART;VALUE=DATE:20261017\nCREATED:00001231T000000Z\n"));
        assertThat(google.getProperty(Property.CREATED)).map(Property::getValue).contains("00001231T000000Z");
        assertThat(google.getProperty(Property.DTSTART)).isPresent();
    }

    /** A hang guard only: ical4j listing a zone's changes without end never comes back, and cannot be interrupted. */
    private static Calendar parseInTime(String ics) {
        return assertTimeoutPreemptively(Duration.ofSeconds(10), () -> IcalSupport.parse(ics));
    }

    private static String zone(String tzId, String... observances) {
        return "BEGIN:VTIMEZONE\nTZID:" + tzId + "\n" + String.join("", observances) + "END:VTIMEZONE\n";
    }

    private static String observance(String kind, String start, String from, String to, String rule) {
        return "BEGIN:" + kind + "\nDTSTART:" + start + "\nTZOFFSETFROM:" + from + "\nTZOFFSETTO:" + to + "\n"
            + (rule == null ? "" : "RRULE:" + rule + "\n") + "END:" + kind + "\n";
    }

    /** How Outlook writes the zone of New York, whatever it names it. */
    private static String outlookEastern(String tzId) {
        return zone(tzId, observance("STANDARD", "16010101T020000", "-0400", "-0500", "FREQ=YEARLY;INTERVAL=1;BYDAY=1SU;BYMONTH=11"),
            observance("DAYLIGHT", "16010101T020000", "-0500", "-0400", "FREQ=YEARLY;INTERVAL=1;BYDAY=2SU;BYMONTH=3"));
    }

    private static String zones(int count) {
        StringBuilder out = new StringBuilder();
        for (int i = 1; i <= count; i++) {
            out.append("BEGIN:VTIMEZONE\nTZID:Zone").append(i)
                .append("\nBEGIN:STANDARD\nTZOFFSETFROM:+0100\nTZOFFSETTO:+0100\nTZNAME:Z\nDTSTART:19700101T000000\nEND:STANDARD\nEND:VTIMEZONE\n");
        }
        return out.toString();
    }
}
