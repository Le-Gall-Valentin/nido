package com.nido.api.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class LoggedPathTest {

    @Test
    void theTokenOfASubscriptionAddressNeverReachesTheLog() {
        assertThat(LoggedPath.of(new MockHttpServletRequest("GET", "/ical/AbCdEfGhIjKlMnOpQrStUvWxYz0123456789_-abcde.ics")))
            .isEqualTo("/ical/***.ics");
    }

    @Test
    void anyOtherPathIsLoggedAsItIs() {
        assertThat(LoggedPath.of(new MockHttpServletRequest("GET", "/api/spaces/1/calendar/occurrences")))
            .isEqualTo("/api/spaces/1/calendar/occurrences");
    }

    private static final String TOKEN = "AbCdEfGhIjKlMnOpQrStUvWxYz0123456789_-abcde";

    @Test
    void aPathParameterDoesNotCarryTheTokenOut() {
        assertThat(path("/ical/" + TOKEN + ".ics;jsessionid=ABC")).isEqualTo("/ical/***.ics");
        assertThat(path("/ical/" + TOKEN + ";x=1.ics")).isEqualTo("/ical/***.ics");
    }

    @Test
    void aPercentEncodedCharacterInTheTokenDoesNotCarryItOut() {
        assertThat(path("/ical/AbCdEfGhIjKlMnOpQrStUvWxYz01234%35" + "6789_-abcde.ics")).isEqualTo("/ical/***.ics");
    }

    @Test
    void anEncodedSpellingOfTheRouteIsMaskedLikeTheRoute() {
        assertThat(path("/%69cal/" + TOKEN + ".ics")).isEqualTo("/ical/***.ics");
        assertThat(path("//ical//" + TOKEN + ".ics")).isEqualTo("/ical/***.ics");
    }

    @Test
    void aPathUnderTheRouteWithoutTheExtensionIsMaskedToo() {
        // The route would answer it with nothing; the token is in it all the same.
        assertThat(path("/ical/" + TOKEN)).isEqualTo("/ical/***.ics");
    }

    @Test
    void aPathThatCannotBeDecodedIsNotLoggedAtAll() {
        assertThat(path("/ical/%zz" + TOKEN + ".ics")).isEqualTo("/***");
    }

    @Test
    void neighbouringPathsAreNotMasked() {
        assertThat(path("/icalendar/" + TOKEN + ".ics")).isEqualTo("/icalendar/" + TOKEN + ".ics");
        assertThat(path("/api/ical/" + TOKEN + ".ics")).isEqualTo("/api/ical/" + TOKEN + ".ics");
        assertThat(path("/ical")).isEqualTo("/ical");
    }

    private static String path(String uri) {
        return LoggedPath.of(new MockHttpServletRequest("GET", uri));
    }
}
