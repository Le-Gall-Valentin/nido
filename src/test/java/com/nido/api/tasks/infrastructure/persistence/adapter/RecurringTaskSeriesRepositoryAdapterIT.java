package com.nido.api.tasks.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.TestSpaces;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.sealing.SealedValueRejected;
import com.nido.api.infrastructure.sealing.SpaceSealer;
import com.nido.api.infrastructure.sealing.SpaceSealers;
import com.nido.api.shared.model.Role;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import com.nido.api.tasks.domain.model.CreateRecurringTaskSeriesCommand;
import com.nido.api.tasks.domain.model.RecurrenceInterval;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.model.TaskPriority;
import com.nido.api.tasks.domain.model.UpdateRecurringTaskSeriesCommand;
import com.nido.api.tasks.infrastructure.persistence.entity.RecurringTaskSeriesEntity;
import com.nido.api.tasks.infrastructure.persistence.entity.RecurringTaskSeriesSubtaskTemplateEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTestConfig
class RecurringTaskSeriesRepositoryAdapterIT {

    @Autowired RecurringTaskSeriesRepositoryAdapter adapter;
    @Autowired SpaceJpaRepository spaceJpaRepository;
    @Autowired UserIdentityJpaRepository userJpaRepository;
    @Autowired JdbcTemplate jdbc;
    @Autowired SpaceSealers sealers;

    private UUID spaceId;
    private UUID aliceId;
    private UUID bobId;

    @BeforeEach
    void setUp() {
        spaceJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        TestSpaces.name(space, "Chez Valentin");
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
    void create_persists_the_series_with_its_rotation_members_subtask_templates_and_creator_in_order() {
        CreateRecurringTaskSeriesCommand command = new CreateRecurringTaskSeriesCommand(
            spaceId, "Sortir les poubelles", TaskPriority.MED, List.of("Vérifier le tri"),
            RecurrenceInterval.WEEKLY, 1, RecurrenceInterval.DAILY, 2,
            LocalDate.of(2026, 1, 7), null, List.of(aliceId, bobId), aliceId);

        RecurringTaskSeries created = adapter.create(command);

        assertThat(created.title()).isEqualTo("Sortir les poubelles");
        assertThat(created.leadIntervalType()).isEqualTo(RecurrenceInterval.DAILY);
        assertThat(created.leadIntervalCount()).isEqualTo(2);
        assertThat(created.endDate()).isNull();
        assertThat(created.rotationMemberIds()).containsExactly(aliceId, bobId);
        assertThat(created.subtaskTemplates()).containsExactly("Vérifier le tri");
        assertThat(created.occurrenceCount()).isZero();
        assertThat(created.currentRotationIndex()).isZero();
        assertThat(created.createdBy()).isEqualTo(aliceId);
    }

    @Test
    void advance_updates_the_rotation_index_and_occurrence_count() {
        RecurringTaskSeries created = adapter.create(new CreateRecurringTaskSeriesCommand(
            spaceId, "Sortir les poubelles", TaskPriority.MED, List.of(),
            RecurrenceInterval.WEEKLY, 1, RecurrenceInterval.DAILY, 0,
            LocalDate.of(2026, 1, 7), null, List.of(aliceId, bobId), aliceId));

        RecurringTaskSeries advanced = adapter.advance(created.id(), 1, 1);

        assertThat(advanced.currentRotationIndex()).isEqualTo(1);
        assertThat(advanced.occurrenceCount()).isEqualTo(1);
    }

    @Test
    void deleteById_removes_the_series() {
        RecurringTaskSeries created = adapter.create(new CreateRecurringTaskSeriesCommand(
            spaceId, "Sortir les poubelles", TaskPriority.MED, List.of(),
            RecurrenceInterval.WEEKLY, 1, RecurrenceInterval.DAILY, 0,
            LocalDate.of(2026, 1, 7), null, List.of(), aliceId));

        adapter.deleteById(created.id());

        assertThat(adapter.findById(created.id())).isEmpty();
    }

    @Test
    void findBySpaceId_returns_every_series_in_the_space() {
        adapter.create(new CreateRecurringTaskSeriesCommand(spaceId, "A", TaskPriority.MED, List.of(),
            RecurrenceInterval.WEEKLY, 1, RecurrenceInterval.DAILY, 0, LocalDate.of(2026, 1, 7), null, List.of(), aliceId));
        adapter.create(new CreateRecurringTaskSeriesCommand(spaceId, "B", TaskPriority.LOW, List.of(),
            RecurrenceInterval.MONTHLY, 1, RecurrenceInterval.DAILY, 0, LocalDate.of(2026, 2, 1), null, List.of(), bobId));

        List<RecurringTaskSeries> found = adapter.findBySpaceId(spaceId);

        assertThat(found).extracting(RecurringTaskSeries::title).containsExactlyInAnyOrder("A", "B");
    }

    @Test
    void update_replaces_the_series_fields_rotation_and_subtask_templates() {
        RecurringTaskSeries created = adapter.create(new CreateRecurringTaskSeriesCommand(
            spaceId, "Sortir les poubelles", TaskPriority.MED, List.of("Vérifier le tri"),
            RecurrenceInterval.WEEKLY, 1, RecurrenceInterval.DAILY, 0,
            LocalDate.of(2026, 1, 7), null, List.of(aliceId), aliceId));

        RecurringTaskSeries updated = adapter.update(new UpdateRecurringTaskSeriesCommand(
            created.id(), spaceId, "Sortir les poubelles et le compost", TaskPriority.HIGH, List.of("Vérifier le tri", "Sortir les bacs"),
            RecurrenceInterval.MONTHLY, 2, RecurrenceInterval.WEEKLY, 1,
            LocalDate.of(2026, 1, 7), LocalDate.of(2027, 1, 1), List.of(bobId, aliceId)));

        assertThat(updated.title()).isEqualTo("Sortir les poubelles et le compost");
        assertThat(updated.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(updated.intervalType()).isEqualTo(RecurrenceInterval.MONTHLY);
        assertThat(updated.intervalCount()).isEqualTo(2);
        assertThat(updated.leadIntervalType()).isEqualTo(RecurrenceInterval.WEEKLY);
        assertThat(updated.leadIntervalCount()).isEqualTo(1);
        assertThat(updated.endDate()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(updated.rotationMemberIds()).containsExactly(bobId, aliceId);
        assertThat(updated.subtaskTemplates()).containsExactly("Vérifier le tri", "Sortir les bacs");
    }

    @Test
    void lockForMaterialization_does_not_throw_when_called_outside_any_prior_lock() {
        adapter.lockForMaterialization(spaceId);
    }

    @Test
    void the_title_and_the_subtask_templates_are_stored_sealed() {
        RecurringTaskSeries created = adapter.create(new CreateRecurringTaskSeriesCommand(spaceId, "Sortir les poubelles",
            TaskPriority.MED, List.of("Trier le verre"), RecurrenceInterval.WEEKLY, 1, RecurrenceInterval.DAILY, 0,
            LocalDate.of(2026, 10, 5), null, List.of(aliceId), aliceId));
        SpaceSealer sealer = sealers.forSpace(spaceId);

        String title = jdbc.queryForObject("SELECT title_encrypted FROM recurring_task_series WHERE id = ?", String.class, created.id());
        assertThat(title).startsWith("v2:");
        assertThat(sealer.open(RecurringTaskSeriesEntity.TITLE, created.id(), title)).isEqualTo("Sortir les poubelles");
        assertThat(jdbc.query("SELECT id, text_encrypted FROM recurring_task_series_subtask_templates WHERE series_id = ?",
                (rs, rowNum) -> sealer.open(RecurringTaskSeriesSubtaskTemplateEntity.TEXT, rs.getObject("id", UUID.class),
                    rs.getString("text_encrypted")), created.id()))
            .containsExactly("Trier le verre");
    }

    @Test
    void a_subtask_template_copied_from_another_one_is_refused() {
        RecurringTaskSeries created = adapter.create(new CreateRecurringTaskSeriesCommand(spaceId, "Sortir les poubelles",
            TaskPriority.MED, List.of("Trier le verre", "Rentrer les bacs"), RecurrenceInterval.WEEKLY, 1, RecurrenceInterval.DAILY, 0,
            LocalDate.of(2026, 10, 5), null, List.of(aliceId), aliceId));
        jdbc.update("""
            UPDATE recurring_task_series_subtask_templates SET text_encrypted =
              (SELECT text_encrypted FROM recurring_task_series_subtask_templates WHERE series_id = ? AND position = 0)
            WHERE series_id = ? AND position = 1""", created.id(), created.id());

        assertThatThrownBy(() -> adapter.findById(created.id())).isInstanceOf(SealedValueRejected.class);
    }

    @Test
    void an_update_seals_under_the_space_of_the_series_whatever_the_command_says() {
        RecurringTaskSeries created = adapter.create(new CreateRecurringTaskSeriesCommand(
            spaceId, "Sortir les poubelles", TaskPriority.MED, List.of("Vérifier le tri"),
            RecurrenceInterval.WEEKLY, 1, RecurrenceInterval.DAILY, 0,
            LocalDate.of(2026, 1, 7), null, List.of(aliceId), aliceId));

        adapter.update(new UpdateRecurringTaskSeriesCommand(
            created.id(), anotherSpace(), "Sortir le compost", TaskPriority.MED, List.of("Rincer le bac"),
            RecurrenceInterval.WEEKLY, 1, RecurrenceInterval.DAILY, 0,
            LocalDate.of(2026, 1, 7), null, List.of(aliceId)));

        RecurringTaskSeries reread = adapter.findById(created.id()).orElseThrow();
        assertThat(reread.title()).isEqualTo("Sortir le compost");
        assertThat(reread.subtaskTemplates()).containsExactly("Rincer le bac");
    }

    private UUID anotherSpace() {
        SpaceEntity other = new SpaceEntity();
        other.setType(SpaceType.SHARED);
        TestSpaces.name(other, "Autre groupe");
        other.setAccent("#c17a5c");
        other.setGlyph("🏡");
        return spaceJpaRepository.saveAndFlush(other).getId();
    }
}
