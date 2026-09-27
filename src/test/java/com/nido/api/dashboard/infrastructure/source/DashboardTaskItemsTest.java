package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskPriority;
import com.nido.api.tasks.domain.model.TaskStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The client reads these names: the dashboard's own enums must spell each one as the tasks module does. */
class DashboardTaskItemsTest {

    @ParameterizedTest
    @EnumSource(TaskPriority.class)
    void everyPriorityKeepsTheNameTheClientReads(TaskPriority priority) {
        assertThat(DashboardTaskItems.from(task(priority, TaskStatus.TODO)).priority().name()).isEqualTo(priority.name());
    }

    @ParameterizedTest
    @EnumSource(TaskStatus.class)
    void everyStatusKeepsTheNameTheClientReads(TaskStatus status) {
        assertThat(DashboardTaskItems.from(task(TaskPriority.MED, status)).status().name()).isEqualTo(status.name());
    }

    private static Task task(TaskPriority priority, TaskStatus status) {
        return new Task(UUID.randomUUID(), UUID.randomUUID(), "Poubelles", status, priority, null, List.of(), List.of(),
            null, UUID.randomUUID(), Instant.parse("2026-01-01T00:00:00Z"));
    }
}
