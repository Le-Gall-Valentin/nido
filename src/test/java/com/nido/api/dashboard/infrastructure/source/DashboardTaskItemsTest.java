package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.dashboard.domain.model.DashboardContext;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskPriority;
import com.nido.api.tasks.domain.model.TaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The client reads these names: the dashboard's own enums must spell each one as the tasks module does. */
class DashboardTaskItemsTest {

    private final UUID me = UUID.randomUUID();
    private final DashboardContext shared = new DashboardContext(
        new SpaceMembership(UUID.randomUUID(), UUID.randomUUID(), me, SpaceRole.MEMBER, Instant.now()),
        LocalDate.of(2026, 9, 26), SpaceType.SHARED);

    @Test
    void anItemKnowsWhetherItIsTheCallersToDo() {
        // Decided once, by DashboardContext#isMine: the triage, the agenda and the client all read it.
        assertThat(DashboardTaskItems.from(task(List.of(me)), shared).mine()).isTrue();
        assertThat(DashboardTaskItems.from(task(List.of()), shared).mine()).isTrue();
        assertThat(DashboardTaskItems.from(task(List.of(UUID.randomUUID())), shared).mine()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(TaskPriority.class)
    void everyPriorityKeepsTheNameTheClientReads(TaskPriority priority) {
        assertThat(DashboardTaskItems.from(task(priority, TaskStatus.TODO), shared).priority().name()).isEqualTo(priority.name());
    }

    @ParameterizedTest
    @EnumSource(TaskStatus.class)
    void everyStatusKeepsTheNameTheClientReads(TaskStatus status) {
        assertThat(DashboardTaskItems.from(task(TaskPriority.MED, status), shared).status().name()).isEqualTo(status.name());
    }

    private static Task task(List<UUID> assignees) {
        return new Task(UUID.randomUUID(), UUID.randomUUID(), "Poubelles", TaskStatus.TODO, TaskPriority.MED, null, assignees,
            List.of(), null, UUID.randomUUID(), Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static Task task(TaskPriority priority, TaskStatus status) {
        return new Task(UUID.randomUUID(), UUID.randomUUID(), "Poubelles", status, priority, null, List.of(), List.of(),
            null, UUID.randomUUID(), Instant.parse("2026-01-01T00:00:00Z"));
    }
}
