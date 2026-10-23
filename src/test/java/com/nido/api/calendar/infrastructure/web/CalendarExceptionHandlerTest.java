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
    void what_was_removed_meanwhile_is_said_under_a_code_of_its_own() {
        var link = handler.handle(new CalendarException.SubscriptionLinkNotFound(),
            new MockHttpServletRequest("DELETE", "/api/spaces/1/calendar/subscription-links/2"));
        var feed = handler.handle(new CalendarException.ExternalFeedNotFound(),
            new MockHttpServletRequest("POST", "/api/spaces/1/calendar/external-feeds/2/refresh"));

        assertThat(link.getStatusCode().value()).isEqualTo(404);
        assertThat(link.getBody().getProperties()).containsEntry("error_code", "subscription_link_not_found");
        assertThat(feed.getStatusCode().value()).isEqualTo(404);
        assertThat(feed.getBody().getProperties()).containsEntry("error_code", "external_feed_not_found");
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
    void a_file_too_large_says_which_limit_it_passes() {
        var events = handler.handle(new CalendarException.ImportTooLarge(CalendarException.ImportTooLarge.Limit.EVENTS, 5_000),
            new MockHttpServletRequest("POST", "/api/spaces/1/calendar/import"));
        var rows = handler.handle(new CalendarException.ImportTooLarge(CalendarException.ImportTooLarge.Limit.ROWS, 10_000),
            new MockHttpServletRequest("POST", "/api/spaces/1/calendar/import"));

        assertThat(events.getBody().getDetail()).isEqualTo("A file imported holds at most 5000 events.");
        assertThat(rows.getBody().getDetail()).startsWith("An import writes at most 10000 rows");
        assertThat(rows.getBody().getProperties()).containsEntry("error_code", "import_too_large");
    }

    @Test
    void any_other_path_is_the_instance_as_it_is() {
        var answer = handler.handle(new CalendarException.EventNotFound(),
            new MockHttpServletRequest("GET", "/api/spaces/1/calendar/events/2"));

        assertThat(answer.getBody().getInstance()).hasToString("/api/spaces/1/calendar/events/2");
    }
}
