package com.nido.api.tasks.application.handler;

import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.tasks.domain.model.Subtask;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToggleSubtaskHandlerTest {

    @Mock TaskRepository taskRepository;
    private ToggleSubtaskHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID taskId = UUID.randomUUID();
    private final UUID subtaskId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ToggleSubtaskHandler(taskRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    private Task task(UUID inSpace) {
        return new Task(taskId, inSpace, "T", TaskStatus.TODO, TaskPriority.LOW, null, List.of(),
            List.of(new Subtask(subtaskId, "Vérifier", false)), null, Instant.now());
    }

    @Test
    void a_member_can_toggle_a_subtask() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task(spaceId)));

        handler.toggle(taskId, subtaskId, spaceId, membership(SpaceRole.MEMBER));

        verify(taskRepository).toggleSubtask(taskId, subtaskId);
    }

    @Test
    void a_task_from_another_space_is_not_found() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task(UUID.randomUUID())));

        assertThatThrownBy(() -> handler.toggle(taskId, subtaskId, spaceId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(TaskException.TaskNotFound.class);
    }

    @Test
    void a_subtask_id_that_does_not_belong_to_the_given_task_is_not_found() {
        // Regression test: the subtask must actually belong to taskId — otherwise a caller who
        // can write to any space could toggle an arbitrary subtask elsewhere (including in a
        // space they aren't even a member of) just by knowing its UUID.
        UUID foreignSubtaskId = UUID.randomUUID();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task(spaceId)));

        assertThatThrownBy(() -> handler.toggle(taskId, foreignSubtaskId, spaceId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(TaskException.TaskNotFound.class);
        verify(taskRepository, org.mockito.Mockito.never()).toggleSubtask(any(), any());
    }

    @Test
    void a_viewer_cannot_toggle_a_subtask() {
        assertThatThrownBy(() -> handler.toggle(taskId, subtaskId, spaceId, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }
}
