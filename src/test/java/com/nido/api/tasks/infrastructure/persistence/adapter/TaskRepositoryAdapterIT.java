package com.nido.api.tasks.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.shared.model.Role;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import com.nido.api.tasks.domain.model.CreateTaskCommand;
import com.nido.api.tasks.domain.model.SubtaskInput;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskPriority;
import com.nido.api.tasks.domain.model.TaskStatus;
import com.nido.api.tasks.domain.model.UpdateTaskCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class TaskRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired TaskRepositoryAdapter adapter;
    @Autowired SpaceJpaRepository spaceJpaRepository;
    @Autowired UserIdentityJpaRepository userJpaRepository;

    private UUID spaceId;
    private UUID aliceId;

    @BeforeEach
    void setUp() {
        spaceJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        space.setName("Chez Valentin");
        space.setAccent("#c17a5c");
        space.setGlyph("🏡");
        spaceId = spaceJpaRepository.saveAndFlush(space).getId();

        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername("alice");
        user.setEmail("alice@test.com");
        user.setRole(Role.USER);
        aliceId = userJpaRepository.saveAndFlush(user).getId();
    }

    @Test
    void create_persists_the_task_with_its_assignees_and_subtasks() {
        Task created = adapter.create(new CreateTaskCommand(spaceId, "Sortir les poubelles", TaskPriority.MED,
            LocalDate.of(2026, 1, 7), List.of(aliceId), List.of(new SubtaskInput("Vérifier le tri", false)), null));

        assertThat(created.title()).isEqualTo("Sortir les poubelles");
        assertThat(created.status()).isEqualTo(TaskStatus.TODO);
        assertThat(created.assigneeIds()).containsExactly(aliceId);
        assertThat(created.subtasks()).hasSize(1);
        assertThat(created.subtasks().get(0).text()).isEqualTo("Vérifier le tri");
        assertThat(created.subtasks().get(0).done()).isFalse();
    }

    @Test
    void update_replaces_the_assignee_list() {
        Task created = adapter.create(new CreateTaskCommand(spaceId, "T", TaskPriority.LOW, null, List.of(aliceId), List.of(), null));

        Task updated = adapter.update(new UpdateTaskCommand(created.id(), spaceId, "T modifié", TaskPriority.HIGH, null, List.of()));

        assertThat(updated.title()).isEqualTo("T modifié");
        assertThat(updated.assigneeIds()).isEmpty();
    }

    @Test
    void updateStatus_changes_only_the_status() {
        Task created = adapter.create(new CreateTaskCommand(spaceId, "T", TaskPriority.LOW, null, List.of(), List.of(), null));

        Task updated = adapter.updateStatus(created.id(), TaskStatus.DOING);

        assertThat(updated.status()).isEqualTo(TaskStatus.DOING);
    }

    @Test
    void toggleSubtask_flips_only_the_targeted_subtask() {
        Task created = adapter.create(new CreateTaskCommand(spaceId, "T", TaskPriority.LOW, null, List.of(),
            List.of(new SubtaskInput("A", false), new SubtaskInput("B", false)), null));
        UUID subtaskAId = created.subtasks().get(0).id();

        Task updated = adapter.toggleSubtask(created.id(), subtaskAId);

        assertThat(updated.subtasks().get(0).done()).isTrue();
        assertThat(updated.subtasks().get(1).done()).isFalse();
    }

    @Test
    void delete_removes_the_task() {
        Task created = adapter.create(new CreateTaskCommand(spaceId, "T", TaskPriority.LOW, null, List.of(), List.of(), null));

        adapter.delete(created.id());

        assertThat(adapter.findById(created.id())).isEmpty();
    }

    @Test
    void findBySpaceId_returns_every_task_in_the_space() {
        adapter.create(new CreateTaskCommand(spaceId, "T1", TaskPriority.LOW, null, List.of(), List.of(), null));
        adapter.create(new CreateTaskCommand(spaceId, "T2", TaskPriority.LOW, null, List.of(), List.of(), null));

        assertThat(adapter.findBySpaceId(spaceId)).hasSize(2);
    }
}
