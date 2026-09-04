package com.nido.api.tasks.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.shared.model.Role;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import com.nido.api.tasks.domain.model.CreateRecurringTaskSeriesCommand;
import com.nido.api.tasks.domain.model.RecurrenceInterval;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.model.TaskPriority;
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
class RecurringTaskSeriesRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired RecurringTaskSeriesRepositoryAdapter adapter;
    @Autowired SpaceJpaRepository spaceJpaRepository;
    @Autowired UserIdentityJpaRepository userJpaRepository;

    private UUID spaceId;
    private UUID aliceId;
    private UUID bobId;

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

        aliceId = saveUser("alice");
        bobId = saveUser("bob");
    }

    private UUID saveUser(String username) {
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setRole(Role.USER);
        return userJpaRepository.saveAndFlush(user).getId();
    }

    @Test
    void create_persists_the_series_with_its_rotation_members_and_subtask_templates_in_order() {
        CreateRecurringTaskSeriesCommand command = new CreateRecurringTaskSeriesCommand(
            spaceId, "Sortir les poubelles", TaskPriority.MED, List.of("Vérifier le tri"),
            RecurrenceInterval.WEEKLY, 1, LocalDate.of(2026, 1, 7), List.of(aliceId, bobId));

        RecurringTaskSeries created = adapter.create(command);

        assertThat(created.title()).isEqualTo("Sortir les poubelles");
        assertThat(created.rotationMemberIds()).containsExactly(aliceId, bobId);
        assertThat(created.subtaskTemplates()).containsExactly("Vérifier le tri");
        assertThat(created.occurrenceCount()).isZero();
        assertThat(created.currentRotationIndex()).isZero();
    }

    @Test
    void advance_updates_the_rotation_index_and_occurrence_count() {
        RecurringTaskSeries created = adapter.create(new CreateRecurringTaskSeriesCommand(
            spaceId, "Sortir les poubelles", TaskPriority.MED, List.of(),
            RecurrenceInterval.WEEKLY, 1, LocalDate.of(2026, 1, 7), List.of(aliceId, bobId)));

        RecurringTaskSeries advanced = adapter.advance(created.id(), 1, 1);

        assertThat(advanced.currentRotationIndex()).isEqualTo(1);
        assertThat(advanced.occurrenceCount()).isEqualTo(1);
    }

    @Test
    void deleteById_removes_the_series() {
        RecurringTaskSeries created = adapter.create(new CreateRecurringTaskSeriesCommand(
            spaceId, "Sortir les poubelles", TaskPriority.MED, List.of(),
            RecurrenceInterval.WEEKLY, 1, LocalDate.of(2026, 1, 7), List.of()));

        adapter.deleteById(created.id());

        assertThat(adapter.findById(created.id())).isEmpty();
    }
}
