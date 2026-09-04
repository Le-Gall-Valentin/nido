package com.nido.api.tasks.domain.model;

public abstract sealed class TaskException extends RuntimeException
    permits TaskException.TaskNotFound, TaskException.SubtasksIncomplete, TaskException.SameSpaceTransfer {

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
}
