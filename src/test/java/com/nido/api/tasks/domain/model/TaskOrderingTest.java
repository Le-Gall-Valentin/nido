package com.nido.api.tasks.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TaskOrderingTest {

    private static Task task(TaskPriority priority, LocalDate dueDate, Instant createdAt) {
        return new Task(UUID.randomUUID(), UUID.randomUUID(), "T", TaskStatus.TODO, priority,
            dueDate, List.of(), List.of(), null, createdAt);
    }

    @Test
    void higher_priority_sorts_first_regardless_of_due_date() {
        Task low = task(TaskPriority.LOW, LocalDate.of(2026, 1, 1), Instant.now());
        Task high = task(TaskPriority.HIGH, LocalDate.of(2026, 12, 31), Instant.now());

        assertThat(TaskOrdering.sort(List.of(low, high))).containsExactly(high, low);
    }

    @Test
    void within_the_same_priority_the_earlier_due_date_sorts_first() {
        Instant now = Instant.now();
        Task later = task(TaskPriority.MED, LocalDate.of(2026, 6, 1), now);
        Task sooner = task(TaskPriority.MED, LocalDate.of(2026, 1, 1), now);

        assertThat(TaskOrdering.sort(List.of(later, sooner))).containsExactly(sooner, later);
    }

    @Test
    void within_the_same_priority_a_task_with_no_due_date_sorts_last() {
        Instant now = Instant.now();
        Task withDate = task(TaskPriority.MED, LocalDate.of(2026, 1, 1), now);
        Task withoutDate = task(TaskPriority.MED, null, now);

        assertThat(TaskOrdering.sort(List.of(withoutDate, withDate))).containsExactly(withDate, withoutDate);
    }

    @Test
    void when_priority_and_due_date_tie_the_earlier_created_task_sorts_first() {
        LocalDate sameDueDate = LocalDate.of(2026, 1, 1);
        Task newer = task(TaskPriority.MED, sameDueDate, Instant.parse("2026-01-02T00:00:00Z"));
        Task older = task(TaskPriority.MED, sameDueDate, Instant.parse("2026-01-01T00:00:00Z"));

        assertThat(TaskOrdering.sort(List.of(newer, older))).containsExactly(older, newer);
    }
}
