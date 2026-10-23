package com.nido.api.calendar.infrastructure.web;

import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.infrastructure.web.LoggedPath;
import com.nido.api.shared.infrastructure.web.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

// Ahead of GlobalExceptionHandler's last resort, which matches Exception and would otherwise be
// picked first on an order tie — turning a declared domain error into a 500.
@Order(Ordered.LOWEST_PRECEDENCE - 100)
@RestControllerAdvice
public class CalendarExceptionHandler {

    @ExceptionHandler(CalendarException.class)
    public ResponseEntity<ProblemDetail> handle(CalendarException e, HttpServletRequest request) {
        CalendarErrorResponse response = switch (e) {
            case CalendarException.EventNotFound ignored ->
                new CalendarErrorResponse(404, "Event not found.");
            case CalendarException.RecurringEventSeriesNotFound ignored ->
                new CalendarErrorResponse(404, "Recurring event series not found.");
            case CalendarException.MemberNotInSpace ignored ->
                new CalendarErrorResponse(404, "Member is not part of this space.");
            case CalendarException.ParticipantsFixedInPersonalSpace ignored ->
                new CalendarErrorResponse(400, "In a personal context you always take part in your events.");
            case CalendarException.OccurrenceNotInSeries ignored ->
                new CalendarErrorResponse(400, "That date is not an occurrence of this series.");
            case CalendarException.SameSpaceTransfer ignored ->
                new CalendarErrorResponse(400, "Cannot transfer an event into its own context.");
            case CalendarException.InvalidTimeRange ignored ->
                new CalendarErrorResponse(400, "An all-day event carries no times, a timed event carries both, and an event never ends before it starts.");
            case CalendarException.EventTooLong ex ->
                new CalendarErrorResponse(400, "An event lasts at most " + ex.maximumDays() + " days.");
            case CalendarException.OccurrenceLongerThanInterval ignored ->
                new CalendarErrorResponse(400, "An occurrence cannot last longer than the time between two occurrences.");
            case CalendarException.IntervalTooLarge ex -> new CalendarErrorResponse(400,
                "A series repeats at an interval of at most " + ex.maximum() + ".", "interval_too_large");
            case CalendarException.InvalidEndDate ignored ->
                new CalendarErrorResponse(400, "The end date must be on or after the anchor date.");
            case CalendarException.InvalidRecurrenceRule ignored -> new CalendarErrorResponse(400,
                "The days of a series are read off its first day: it must be one of them.", "invalid_recurrence_rule");
            case CalendarException.NotAnIcalFile ignored ->
                new CalendarErrorResponse(400, "This is not an iCalendar (.ics) document.", "not_ics");
            case CalendarException.InvalidSources ignored -> new CalendarErrorResponse(400,
                "Choose at least one source among events, tasks, meals, money and savings.", "invalid_sources");
            case CalendarException.TooManySubscriptionLinks ex -> new CalendarErrorResponse(409,
                "You already have " + ex.maximum() + " addresses for this space. Revoke one first.", "too_many_links");
            case CalendarException.SubscriptionLinkNotFound ignored ->
                new CalendarErrorResponse(404, "Subscription address not found.");
            case CalendarException.PublicUrlMissing ignored -> new CalendarErrorResponse(409,
                "The public address of this installation is not set: an administrator sets it in the settings.",
                "public_url_missing");
            case CalendarException.ImportTooLarge ex ->
                new CalendarErrorResponse(400, ex.getMessage() + ".", "import_too_large");
            case CalendarException.ImportFileTooLarge ex -> new CalendarErrorResponse(413,
                "Files up to " + ex.maximumMegabytes() + " MB can be imported.", "file_too_large");
            case CalendarException.InvalidFeedAddress ignored ->
                new CalendarErrorResponse(400, "Give an http, https or webcal address.", "invalid_feed_address");
            case CalendarException.ExternalFeedNotFound ignored ->
                new CalendarErrorResponse(404, "Followed calendar not found.");
            case CalendarException.FeedUnavailable ex -> new CalendarErrorResponse(400,
                "The calendar could not be read at this address (" + ex.error().code() + ").", "feed_unavailable");
            case CalendarException.TooManyExternalFeeds ex -> new CalendarErrorResponse(409,
                "This space already follows " + ex.maximum() + " calendars.", "too_many_feeds");
            case CalendarException.RefreshTooSoon ignored ->
                new CalendarErrorResponse(429, "This calendar was read less than a minute ago.", "refresh_too_soon");
            case CalendarException.ReversedWindow ignored ->
                new CalendarErrorResponse(400, "The window starts after it ends: 'from' must be on or before 'to'.");
            case CalendarException.WindowTooLarge ex -> new CalendarErrorResponse(400,
                "This window covers " + ex.requestedDays() + " days (maximum " + ex.maximum()
                    + "). Ask for a shorter period.");
        };
        ProblemDetail problem = ProblemDetailFactory.of(
            HttpStatus.valueOf(response.status()), e.getClass().getSimpleName(), response.detail(),
            URI.create(LoggedPath.of(request)));
        if (response.errorCode() != null) {
            problem.setProperty("error_code", response.errorCode());
        }
        if (e instanceof CalendarException.FeedUnavailable unavailable) {
            problem.setProperty("feed_error", unavailable.error().code());
        }
        return ResponseEntity.status(response.status()).body(problem);
    }

    private record CalendarErrorResponse(int status, String detail, String errorCode) {
        CalendarErrorResponse(int status, String detail) {
            this(status, detail, null);
        }
    }
}
