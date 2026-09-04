package com.nido.api.tasks.domain.model;

// Declaration order is significant: Task 2's ordering sorts by this enum's
// natural (ordinal) order, so HIGH must sort before MED before LOW.
public enum TaskPriority {
    HIGH, MED, LOW
}
