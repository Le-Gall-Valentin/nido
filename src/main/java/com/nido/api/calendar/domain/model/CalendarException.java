package com.nido.api.calendar.domain.model;

public abstract sealed class CalendarException extends RuntimeException
    permits CalendarException.EventNotFound, CalendarException.RecurringEventSeriesNotFound,
            CalendarException.OccurrenceNotInSeries, CalendarException.SameSpaceTransfer,
            CalendarException.InvalidTimeRange, CalendarException.InvalidEndDate,
            CalendarException.MemberNotInSpace, CalendarException.WindowTooLarge,
            CalendarException.ParticipantsFixedInPersonalSpace, CalendarException.EventTooLong,
            CalendarException.OccurrenceLongerThanInterval, CalendarException.ReversedWindow {

    private CalendarException(String message) { super(message); }

    public static final class EventNotFound extends CalendarException {
        public EventNotFound() { super("Event not found"); }
    }

    public static final class RecurringEventSeriesNotFound extends CalendarException {
        public RecurringEventSeriesNotFound() { super("Recurring event series not found"); }
    }

    /**
     * Thrown when a caller edits or cancels a date the series never produces. Without it, a
     * typo'd date would create an event no series ever emits — visible in the calendar, tied
     * to a series, and impossible to reach again from that series' own occurrences.
     */
    public static final class OccurrenceNotInSeries extends CalendarException {
        public OccurrenceNotInSeries() { super("That date is not an occurrence of this series"); }
    }

    /** Thrown when a copy or move targets the context the event is already in. */
    public static final class SameSpaceTransfer extends CalendarException {
        public SameSpaceTransfer() { super("Cannot transfer an event into its own context"); }
    }

    /** Thrown when all-day and the times disagree, or an end falls before its start. */
    public static final class InvalidTimeRange extends CalendarException {
        public InvalidTimeRange() { super("Invalid time range"); }
    }

    /**
     * Thrown when an event spans more days than a calendar read may show. Beyond being no calendar's
     * business, an event spanning centuries froze every view that drew it, day by day.
     */
    public static final class EventTooLong extends CalendarException {
        private final int maximumDays;

        public EventTooLong(int maximumDays) {
            super("An event lasts at most " + maximumDays + " days");
            this.maximumDays = maximumDays;
        }

        public int maximumDays() { return maximumDays; }
    }

    /**
     * Thrown when each occurrence of a series would last longer than the time between two of them,
     * so that one runs into the next. Besides meaning nothing, it is what let a series last a million
     * days and project hundreds of thousands of occurrences into every read.
     */
    public static final class OccurrenceLongerThanInterval extends CalendarException {
        public OccurrenceLongerThanInterval() { super("An occurrence cannot last longer than the time between two occurrences"); }
    }

    public static final class InvalidEndDate extends CalendarException {
        public InvalidEndDate() { super("The end date must be on or after the anchor date"); }
    }

    public static final class MemberNotInSpace extends CalendarException {
        public MemberNotInSpace() { super("Member is not part of this space"); }
    }

    /**
     * Thrown when someone joins or leaves an event of a personal space. Its owner is always the one
     * participant of everything in it, so there is nothing to join and nothing to leave.
     */
    public static final class ParticipantsFixedInPersonalSpace extends CalendarException {
        public ParticipantsFixedInPersonalSpace() { super("In a personal space, its owner always takes part"); }
    }

    /** Thrown when a read's window starts after it ends: a client bug, named as such. */
    public static final class ReversedWindow extends CalendarException {
        public ReversedWindow() { super("The window starts after it ends"); }
    }

    /**
     * Thrown when a read asks for more days than a calendar view could ever display. The cap is
     * what keeps the endpoint a calendar rather than a full export: every source is queried per
     * request, so an unbounded window is an unbounded amount of work.
     */
    public static final class WindowTooLarge extends CalendarException {
        private final int requestedDays;
        private final int maximum;

        public WindowTooLarge(int requestedDays, int maximum) {
            super("Window too large: " + requestedDays + " days requested, maximum " + maximum);
            this.requestedDays = requestedDays;
            this.maximum = maximum;
        }

        public int requestedDays() { return requestedDays; }
        public int maximum() { return maximum; }
    }
}
