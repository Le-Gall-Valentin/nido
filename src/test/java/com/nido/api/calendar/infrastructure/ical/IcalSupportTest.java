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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.Temporal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        int before = ZoneRulesProviderImpl.INSTANCE.getZoneIdPool().availableZoneIds();
        Calendar parsed = IcalSupport.parse(document(repeated, "DTSTART;TZID=Zone1:20270310T090000\n"));
        int after = ZoneRulesProviderImpl.INSTANCE.getZoneIdPool().availableZoneIds();
        assertThat(parsed).isNotNull();
        assertThat(before - after).isLessThanOrEqualTo(1);
    }

    @Test
    void anUnreadableValueOfAPropertyNidoNeverReadsIsKeptAsWritten() {
        VEvent google = event(document("", "DTSTART;VALUE=DATE:20261017\nCREATED:00001231T000000Z\n"));
        assertThat(google.getProperty(Property.CREATED)).map(Property::getValue).contains("00001231T000000Z");
        assertThat(google.getProperty(Property.DTSTART)).isPresent();
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
