package com.nido.api.calendar.domain.model;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedAddressTest {

    @Test
    void aWebcalAddressIsReadOverHttps() {
        assertThat(FeedAddress.parse("  webcal://calendar.google.com/calendar/ical/x/basic.ics "))
            .isEqualTo(URI.create("https://calendar.google.com/calendar/ical/x/basic.ics"));
    }

    @Test
    void httpAndHttpsAreTakenAsTheyAre() {
        assertThat(FeedAddress.parse("http://ecole.example/vacances.ics")).isEqualTo(URI.create("http://ecole.example/vacances.ics"));
    }

    @Test
    void anythingElseIsRefused() {
        for (String refused : new String[] {"ftp://x.example/a.ics", "file:///etc/passwd", "https://", "x.ics",
            "https://user:secret@x.example/a.ics", "https://x.example/" + "a".repeat(2001), ""}) {
            assertThatThrownBy(() -> FeedAddress.parse(refused)).as(refused).isInstanceOf(CalendarException.InvalidFeedAddress.class);
        }
    }
}
