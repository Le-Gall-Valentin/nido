package com.nido.api.tasks.application.handler;

import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.tasks.domain.model.CreateTaskCommand;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTaskHandlerTest {

    @Mock TaskRepository taskRepository;
    private CreateTaskHandler handler;
    private final UUID spaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new CreateTaskHandler(taskRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    @Test
    void a_member_can_create_a_one_off_task() {
        CreateTaskCommand command = new CreateTaskCommand(spaceId, "Prendre RDV", TaskPriority.HIGH, null, List.of(), List.of(), null);
        Task created = new Task(UUID.randomUUID(), spaceId, "Prendre RDV", TaskStatus.TODO, TaskPriority.HIGH, null, List.of(), List.of(), null, Instant.now());
        when(taskRepository.create(command)).thenReturn(created);

        Task result = handler.create(command, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(created);
    }

    @Test
    void a_viewer_cannot_create_a_task() {
        CreateTaskCommand command = new CreateTaskCommand(spaceId, "Prendre RDV", TaskPriority.HIGH, null, List.of(), List.of(), null);

        assertThatThrownBy(() -> handler.create(command, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }

    @Test
    void creating_a_task_for_another_space_than_the_callers_is_rejected() {
        CreateTaskCommand command = new CreateTaskCommand(UUID.randomUUID(), "Prendre RDV", TaskPriority.HIGH, null, List.of(), List.of(), null);

        assertThatThrownBy(() -> handler.create(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(SpaceException.NotAMember.class);
    }
}
