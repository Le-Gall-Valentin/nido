package com.nido.api.tasks.domain.model;

public abstract sealed class TaskException extends RuntimeException
    permits TaskException.TaskNotFound, TaskException.SubtasksIncomplete, TaskException.SameSpaceTransfer,
            TaskException.RecurringSeriesNotFound, TaskException.LeadTimeExceedsInterval, TaskException.InvalidEndDate,
            TaskException.MemberNotInSpace, TaskException.RecurrenceBacklogTooLarge {

    private TaskException(String message) { super(message); }

    public static final class TaskNotFound extends TaskException {
        public TaskNotFound() { super("Task not found"); }
    }

    /** Thrown when a task is moved to DONE while at least one subtask is still open. */
    public static final class SubtasksIncomplete extends TaskException {
        public SubtasksIncomplete() { super("All subtasks must be done before completing this task"); }
    }

    /** Thrown when a move targets the same context the task is already in. */
    public static final class SameSpaceTransfer extends TaskException {
        public SameSpaceTransfer() { super("Cannot transfer a task into its own context"); }
    }

    public static final class RecurringSeriesNotFound extends TaskException {
        public RecurringSeriesNotFound() { super("Recurring series not found"); }
    }

    /** Thrown when a series' lead time would open an occurrence's window before the previous one's due date. */
    public static final class LeadTimeExceedsInterval extends TaskException {
        public LeadTimeExceedsInterval() { super("The lead time cannot exceed the recurrence interval"); }
    }

    /** Thrown when a recurring series' end date is before its anchor date — it would never produce a single occurrence. */
    public static final class InvalidEndDate extends TaskException {
        public InvalidEndDate() { super("The end date must be on or after the anchor date"); }
    }

    /** Thrown when a submitted rotationMemberId isn't actually a member of the space. */
    public static final class MemberNotInSpace extends TaskException {
        public MemberNotInSpace() { super("Member is not part of this space"); }
    }

    /**
     * Thrown when a recurring series would have to catch up more past occurrences at once
     * than {@link com.nido.api.tasks.domain.model.RecurrenceScheduler#MAX_BACKLOG_OCCURRENCES}
     * allows — an anchor date far enough in the past that materializing it would flood the
     * space with tasks nobody asked for.
     */
    public static final class RecurrenceBacklogTooLarge extends TaskException {
        private final long pendingOccurrences;
        private final int maximum;

        public RecurrenceBacklogTooLarge(long pendingOccurrences, int maximum) {
            super("This recurring series would generate " + pendingOccurrences
                + " past occurrences at once, which is more than the " + maximum + " allowed");
            this.pendingOccurrences = pendingOccurrences;
            this.maximum = maximum;
        }

        public long pendingOccurrences() { return pendingOccurrences; }

        public int maximum() { return maximum; }
    }
}
