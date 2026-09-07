package com.nido.api.tasks.application.handler;

import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskException;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteTaskHandlerTest {

    @Mock TaskRepository taskRepository;
    private DeleteTaskHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID taskId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new DeleteTaskHandler(taskRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    private Task task(UUID recurringSeriesId) {
        return new Task(taskId, spaceId, "T", TaskStatus.TODO, TaskPriority.MED, null, List.of(), List.of(), recurringSeriesId, null, Instant.now());
    }

    @Test
    void deleting_a_one_off_task_deletes_the_task_row() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task(null)));

        handler.delete(taskId, spaceId, membership(SpaceRole.MEMBER));

        verify(taskRepository).delete(taskId);
    }

    @Test
    void deleting_a_materialized_recurring_occurrence_deletes_only_the_task_row_and_leaves_the_series_untouched() {
        UUID seriesId = UUID.randomUUID();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task(seriesId)));

        handler.delete(taskId, spaceId, membership(SpaceRole.MEMBER));

        verify(taskRepository).delete(taskId);
    }

    @Test
    void a_task_from_another_space_is_not_found() {
        Task other = new Task(taskId, UUID.randomUUID(), "T", TaskStatus.TODO, TaskPriority.MED, null, List.of(), List.of(), null, null, Instant.now());
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> handler.delete(taskId, spaceId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(TaskException.TaskNotFound.class);
    }

    @Test
    void a_viewer_cannot_delete_a_task() {
        assertThatThrownBy(() -> handler.delete(taskId, spaceId, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }
}
