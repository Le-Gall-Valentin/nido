package com.nido.api.tasks.application.handler;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskException;
import com.nido.api.tasks.domain.model.TaskPriority;
import com.nido.api.tasks.domain.model.TaskStatus;
import com.nido.api.tasks.domain.model.UpdateTaskCommand;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateTaskHandlerTest {

    @Mock TaskRepository taskRepository;
    private UpdateTaskHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID taskId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new UpdateTaskHandler(taskRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    private Task task(UUID inSpace) {
        return new Task(taskId, inSpace, "Ancien titre", TaskStatus.TODO, TaskPriority.LOW, null, List.of(), List.of(), null, Instant.now());
    }

    @Test
    void a_member_can_update_a_task_in_their_space() {
        UpdateTaskCommand command = new UpdateTaskCommand(taskId, spaceId, "Nouveau titre", TaskPriority.HIGH, null, List.of());
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task(spaceId)));
        Task updated = new Task(taskId, spaceId, "Nouveau titre", TaskStatus.TODO, TaskPriority.HIGH, null, List.of(), List.of(), null, Instant.now());
        when(taskRepository.update(command)).thenReturn(updated);

        Task result = handler.update(command, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(updated);
    }

    @Test
    void a_task_from_another_space_is_not_found() {
        UpdateTaskCommand command = new UpdateTaskCommand(taskId, spaceId, "Nouveau titre", TaskPriority.HIGH, null, List.of());
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task(UUID.randomUUID())));

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(TaskException.TaskNotFound.class);
    }

    @Test
    void a_viewer_cannot_update_a_task() {
        UpdateTaskCommand command = new UpdateTaskCommand(taskId, spaceId, "Nouveau titre", TaskPriority.HIGH, null, List.of());

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.VIEWER)))
            .isInstanceOf(com.nido.api.space.domain.model.SpaceException.InsufficientRole.class);
    }
}
