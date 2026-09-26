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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListOpenTasksHandlerTest {

    @Mock TaskRepository taskRepository;

    private ListOpenTasksHandler handler;
    private final UUID spaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ListOpenTasksHandler(taskRepository);
    }

    @Test
    void returns_the_open_tasks_in_board_order_priority_then_due_date_with_undated_last() {
        Task lowDated = task(TaskPriority.LOW, TaskStatus.TODO, LocalDate.of(2026, 1, 5));
        Task highUndatedDoing = task(TaskPriority.HIGH, TaskStatus.DOING, null);
        Task highDated = task(TaskPriority.HIGH, TaskStatus.TODO, LocalDate.of(2026, 1, 9));
        when(taskRepository.findOpenBySpaceId(spaceId)).thenReturn(List.of(lowDated, highUndatedDoing, highDated));

        assertThat(handler.list(membership())).containsExactly(highDated, highUndatedDoing, lowDated);
    }

    @Test
    void reads_only_the_open_tasks_and_never_the_whole_board() {
        when(taskRepository.findOpenBySpaceId(spaceId)).thenReturn(List.of());

        handler.list(membership());

        verify(taskRepository).findOpenBySpaceId(spaceId);
        verifyNoMoreInteractions(taskRepository);
    }

    private SpaceMembership membership() {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.VIEWER, Instant.now());
    }

    private Task task(TaskPriority priority, TaskStatus status, LocalDate dueDate) {
        return new Task(UUID.randomUUID(), spaceId, "T", status, priority, dueDate, List.of(), List.of(), null, null, Instant.now());
    }
}
