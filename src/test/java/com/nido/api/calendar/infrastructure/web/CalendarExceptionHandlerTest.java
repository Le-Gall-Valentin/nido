package com.nido.api.calendar.infrastructure.web;

import com.nido.api.calendar.domain.model.CalendarException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.HashMap;
import java.util.Map;

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
    void each_reason_a_rule_is_refused_for_is_said_under_one_code() {
        Map<String, String> details = new HashMap<>();
        for (CalendarException.InvalidRecurrenceRule.Reason reason : CalendarException.InvalidRecurrenceRule.Reason.values()) {
            var answer = handler.handle(new CalendarException.InvalidRecurrenceRule(reason),
                new MockHttpServletRequest("POST", "/api/spaces/1/calendar/recurring-event-series"));

            assertThat(answer.getStatusCode().value()).isEqualTo(400);
            assertThat(answer.getBody().getProperties()).containsEntry("error_code", "invalid_recurrence_rule");
            details.put(reason.name(), answer.getBody().getDetail());
        }
        assertThat(details.values()).doesNotHaveDuplicates();
        // A day rule its first day cannot give, as the report found it: nothing about weekdays.
        assertThat(details.get("DAY_RULE_NOT_FROM_ANCHOR")).doesNotContain("week");
    }

    @Test
    void any_other_path_is_the_instance_as_it_is() {
        var answer = handler.handle(new CalendarException.EventNotFound(),
            new MockHttpServletRequest("GET", "/api/spaces/1/calendar/events/2"));

        assertThat(answer.getBody().getInstance()).hasToString("/api/spaces/1/calendar/events/2");
    }
}
