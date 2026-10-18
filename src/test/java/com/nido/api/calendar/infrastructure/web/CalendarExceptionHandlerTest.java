package com.nido.api.calendar.infrastructure.web;

import com.nido.api.calendar.domain.model.CalendarException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarExceptionHandlerTest {

    private final CalendarExceptionHandler handler = new CalendarExceptionHandler();

    @Test
    void the_answer_never_names_the_token_of_a_subscription_address() {
        var answer = handler.handle(new CalendarException.SubscriptionLinkNotFound(),
            new MockHttpServletRequest("GET", "/ical/AbCdEfGhIjKlMnOpQrStUvWxYz0123456789_-abcde.ics"));

        assertThat(answer.getStatusCode().value()).isEqualTo(404);
        assertThat(answer.getBody().getInstance()).hasToString("/ical/***.ics");
    }

    @Test
    void any_other_path_is_the_instance_as_it_is() {
        var answer = handler.handle(new CalendarException.EventNotFound(),
            new MockHttpServletRequest("GET", "/api/spaces/1/calendar/events/2"));

        assertThat(answer.getBody().getInstance()).hasToString("/api/spaces/1/calendar/events/2");
    }
}
