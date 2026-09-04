package com.nido.api.tasks.application.handler;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskPriority;
import com.nido.api.tasks.domain.model.TaskStatus;
import com.nido.api.tasks.domain.port.out.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListTasksHandlerTest {

    @Mock TaskRepository taskRepository;
    private ListTasksHandler handler;
    private final UUID spaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ListTasksHandler(taskRepository);
    }

    private SpaceMembership membership() {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());
    }

    private Task task(TaskPriority priority) {
        return new Task(UUID.randomUUID(), spaceId, "T", TaskStatus.TODO, priority, null, List.of(), List.of(), null, Instant.now());
    }

    @Test
    void lists_the_callers_space_tasks_ordered_by_priority() {
        Task low = task(TaskPriority.LOW);
        Task high = task(TaskPriority.HIGH);
        when(taskRepository.findBySpaceId(spaceId)).thenReturn(List.of(low, high));

        List<Task> result = handler.list(membership());

        assertThat(result).containsExactly(high, low);
    }
}
