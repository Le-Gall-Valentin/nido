package com.nido.api.calendar.domain.model;

public abstract sealed class CalendarException extends RuntimeException
    permits CalendarException.EventNotFound, CalendarException.RecurringEventSeriesNotFound,
            CalendarException.OccurrenceNotInSeries, CalendarException.SameSpaceTransfer,
            CalendarException.InvalidTimeRange, CalendarException.InvalidEndDate,
            CalendarException.MemberNotInSpace, CalendarException.WindowTooLarge,
            CalendarException.ParticipantsFixedInPersonalSpace, CalendarException.EventTooLong,
            CalendarException.OccurrenceLongerThanInterval, CalendarException.IntervalTooLarge,
            CalendarException.ReversedWindow,
            CalendarException.InvalidRecurrenceRule, CalendarException.NotAnIcalFile,
            CalendarException.InvalidSources, CalendarException.TooManySubscriptionLinks,
            CalendarException.SubscriptionLinkNotFound, CalendarException.PublicUrlMissing,
            CalendarException.ImportTooLarge, CalendarException.ImportFileTooLarge,
            CalendarException.InvalidFeedAddress, CalendarException.ExternalFeedNotFound,
            CalendarException.FeedUnavailable, CalendarException.TooManyExternalFeeds,
            CalendarException.RefreshTooSoon {

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

    /**
     * Thrown when a series repeats more rarely than nido can say. Every billion years, a series reached past the last
     * date a calendar holds, and every read of its space failed.
     */
    public static final class IntervalTooLarge extends CalendarException {
        private final int maximum;

        public IntervalTooLarge(int maximum) {
            super("A series repeats at an interval of at most " + maximum);
            this.maximum = maximum;
        }

        public int maximum() { return maximum; }
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

    /**
     * Thrown when a series' days cannot be read off its anchor — a last day that is not one, a fifth weekday, weekdays
     * without the anchor's — or belong to another frequency: the series would not start on its own first occurrence.
     * {@link #reason()} says which.
     */
    public static final class InvalidRecurrenceRule extends CalendarException {
        public enum Reason {
            /** Weekdays given to a series that does not repeat weekly. */
            WEEKDAYS_NOT_WEEKLY,
            /** Weekdays that leave out the anchor's own. */
            ANCHOR_NOT_AMONG_WEEKDAYS,
            /** A day rule given to a series that repeats daily or weekly. */
            DAY_RULE_NOT_MONTHLY_OR_YEARLY,
            /** A day rule the anchor does not fall on: a last day that is not one, a fifth weekday. */
            DAY_RULE_NOT_FROM_ANCHOR
        }

        private final Reason reason;

        public InvalidRecurrenceRule(Reason reason) {
            super("The recurrence rule does not fit the series' first day: " + reason);
            this.reason = reason;
        }

        public Reason reason() { return reason; }
    }

    /** Thrown when a file or a followed address does not hold an iCalendar document nido can read. */
    public static final class NotAnIcalFile extends CalendarException {
        public NotAnIcalFile() { super("Not an iCalendar document"); }
    }

    /** Thrown when an export or an address names no source, or one nido does not export (a followed calendar). */
    public static final class InvalidSources extends CalendarException {
        public InvalidSources() { super("Choose at least one of the calendar's own sources"); }
    }

    /** Thrown when a member already holds as many addresses for a space as one may. */
    public static final class TooManySubscriptionLinks extends CalendarException {
        private final int maximum;

        public TooManySubscriptionLinks(int maximum) {
            super("At most " + maximum + " subscription addresses per person and space");
            this.maximum = maximum;
        }

        public int maximum() { return maximum; }
    }

    /** Thrown for an address that does not exist, or is not the caller's: the two are not told apart. */
    public static final class SubscriptionLinkNotFound extends CalendarException {
        public SubscriptionLinkNotFound() { super("Subscription address not found"); }
    }

    /** Thrown when nido does not know the address it is reached at, which every subscription address begins with. */
    public static final class PublicUrlMissing extends CalendarException {
        public PublicUrlMissing() { super("The public address of this installation is not set"); }
    }

    /** Thrown when a file holds more than an import takes at once: 5 000 events, or 10 000 rows to write. */
    public static final class ImportTooLarge extends CalendarException {
        /** Which limit the file passes. */
        public enum Limit { EVENTS, ROWS }

        private final Limit limit;
        private final int maximum;

        public ImportTooLarge(Limit limit, int maximum) {
            super("This file is too large to import: more than " + maximum + " " + limit.name().toLowerCase());
            this.limit = limit;
            this.maximum = maximum;
        }

        public Limit limit() { return limit; }

        public int maximum() { return maximum; }
    }

    /** Thrown when the address of a calendar to follow is not an http(s) address of a host, without credentials. */
    public static final class InvalidFeedAddress extends CalendarException {
        public InvalidFeedAddress() { super("Not an address a calendar can be followed at"); }
    }

    /** Thrown when an uploaded file is larger than an import reads at all. */
    public static final class ImportFileTooLarge extends CalendarException {
        private final int maximumMegabytes;

        public ImportFileTooLarge(int maximumMegabytes) {
            super("Files up to " + maximumMegabytes + " MB can be imported");
            this.maximumMegabytes = maximumMegabytes;
        }

        public int maximumMegabytes() { return maximumMegabytes; }
    }

    public static final class ExternalFeedNotFound extends CalendarException {
        public ExternalFeedNotFound() { super("Followed calendar not found"); }
    }

    /** Thrown when a calendar to follow could not be read at its address; {@link #error()} says why. */
    public static final class FeedUnavailable extends CalendarException {
        private final FeedError error;

        public FeedUnavailable(FeedError error) {
            super("The calendar could not be read: " + error.code());
            this.error = error;
        }

        public FeedError error() { return error; }
    }

    public static final class TooManyExternalFeeds extends CalendarException {
        private final int maximum;

        public TooManyExternalFeeds(int maximum) {
            super("At most " + maximum + " followed calendars per space");
            this.maximum = maximum;
        }

        public int maximum() { return maximum; }
    }

    /** Thrown when a calendar is asked for again within a minute of the last time. */
    public static final class RefreshTooSoon extends CalendarException {
        public RefreshTooSoon() { super("This calendar was read less than a minute ago"); }
    }
}
