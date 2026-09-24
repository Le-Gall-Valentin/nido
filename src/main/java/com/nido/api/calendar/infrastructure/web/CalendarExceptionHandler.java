package com.nido.api.calendar.infrastructure.web;

import com.nido.api.calendar.domain.model.CalendarException;
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
            case CalendarException.InvalidEndDate ignored ->
                new CalendarErrorResponse(400, "The end date must be on or after the anchor date.");
            case CalendarException.WindowTooLarge ex -> new CalendarErrorResponse(400,
                "This window covers " + ex.requestedDays() + " days (maximum " + ex.maximum()
                    + "). Ask for a shorter period.");
        };
        ProblemDetail problem = ProblemDetailFactory.of(
            HttpStatus.valueOf(response.status()), e.getClass().getSimpleName(), response.detail(),
            URI.create(request.getRequestURI()));
        return ResponseEntity.status(response.status()).body(problem);
    }

    private record CalendarErrorResponse(int status, String detail) {}
}
