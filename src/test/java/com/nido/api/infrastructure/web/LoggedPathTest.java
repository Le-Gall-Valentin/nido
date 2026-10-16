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
}
