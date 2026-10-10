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
import com.nido.api.tasks.domain.model.CreateTaskCommand;
import com.nido.api.tasks.domain.model.Subtask;
import com.nido.api.tasks.domain.model.SubtaskEdit;
import com.nido.api.tasks.domain.model.SubtaskInput;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskPriority;
import com.nido.api.tasks.domain.model.TaskStatus;
import com.nido.api.tasks.domain.model.UpdateTaskCommand;
import com.nido.api.tasks.infrastructure.persistence.entity.TaskEntity;
import com.nido.api.tasks.infrastructure.persistence.entity.TaskSubtaskEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@IntegrationTestConfig
class TaskRepositoryAdapterIT {

    @Autowired TaskRepositoryAdapter adapter;
    @Autowired SpaceJpaRepository spaceJpaRepository;
    @Autowired UserIdentityJpaRepository userJpaRepository;
    @Autowired JdbcTemplate jdbc;
    @Autowired SpaceSealers sealers;

    private UUID spaceId;
    private UUID aliceId;

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

        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername("alice");
        user.setEmail("alice@test.com");
        user.setRole(Role.USER);
        aliceId = userJpaRepository.saveAndFlush(user).getId();
    }

    @Test
    void create_persists_the_task_with_its_assignees_subtasks_and_creator() {
        Task created = adapter.create(new CreateTaskCommand(spaceId, "Sortir les poubelles", TaskPriority.MED,
            LocalDate.of(2026, 1, 7), List.of(aliceId), List.of(new SubtaskInput("Vérifier le tri", false)), null, aliceId));

        assertThat(created.title()).isEqualTo("Sortir les poubelles");
        assertThat(created.status()).isEqualTo(TaskStatus.TODO);
        assertThat(created.assigneeIds()).containsExactly(aliceId);
        assertThat(created.subtasks()).hasSize(1);
        assertThat(created.subtasks().get(0).text()).isEqualTo("Vérifier le tri");
        assertThat(created.subtasks().get(0).done()).isFalse();
        assertThat(created.createdBy()).isEqualTo(aliceId);
    }

    @Test
    void update_replaces_the_assignee_list() {
        Task created = adapter.create(new CreateTaskCommand(spaceId, "T", TaskPriority.LOW, null, List.of(aliceId), List.of(), null, null));

        Task updated = adapter.update(new UpdateTaskCommand(created.id(), spaceId, "T modifié", TaskPriority.HIGH, null, List.of(), null));

        assertThat(updated.title()).isEqualTo("T modifié");
        assertThat(updated.assigneeIds()).isEmpty();
    }

    @Test
    void update_rewrites_the_subtask_list_in_the_order_given() {
        // A and C checked, B not. The edit puts C (renamed) before A, drops B and adds one: every
        // move the form can make, at once, so a check carried by position instead of by id shows.
        Task created = adapter.create(new CreateTaskCommand(spaceId, "T", TaskPriority.LOW, null, List.of(),
            List.of(new SubtaskInput("A", true), new SubtaskInput("B", false), new SubtaskInput("C", true)), null, null));
        UUID a = created.subtasks().get(0).id();
        UUID c = created.subtasks().get(2).id();

        Task updated = adapter.update(new UpdateTaskCommand(created.id(), spaceId, "T", TaskPriority.LOW, null, List.of(),
            List.of(new SubtaskEdit(c, "C renommée"), new SubtaskEdit(a, "A"), new SubtaskEdit(null, "Nouvelle"))));

        assertThat(updated.subtasks())
            .extracting(Subtask::text, Subtask::done)
            .containsExactly(tuple("C renommée", true), tuple("A", true), tuple("Nouvelle", false));
        assertThat(updated.subtasks().subList(0, 2)).extracting(Subtask::id)
            .as("a kept subtask keeps its id: a tick sent from another screen still finds it")
            .containsExactly(c, a);
    }

    @Test
    void update_without_a_subtask_list_leaves_the_subtasks_as_they_are() {
        Task created = adapter.create(new CreateTaskCommand(spaceId, "T", TaskPriority.LOW, null, List.of(),
            List.of(new SubtaskInput("A", true), new SubtaskInput("B", false)), null, null));

        Task updated = adapter.update(new UpdateTaskCommand(created.id(), spaceId, "T modifié", TaskPriority.LOW, null, List.of(), null));

        assertThat(updated.subtasks()).isEqualTo(created.subtasks());
    }

    @Test
    void updateStatus_changes_only_the_status() {
        Task created = adapter.create(new CreateTaskCommand(spaceId, "T", TaskPriority.LOW, null, List.of(), List.of(), null, null));

        Task updated = adapter.updateStatus(created.id(), TaskStatus.DOING);

        assertThat(updated.status()).isEqualTo(TaskStatus.DOING);
    }

    @Test
    void toggleSubtask_flips_only_the_targeted_subtask() {
        Task created = adapter.create(new CreateTaskCommand(spaceId, "T", TaskPriority.LOW, null, List.of(),
            List.of(new SubtaskInput("A", false), new SubtaskInput("B", false)), null, null));
        UUID subtaskAId = created.subtasks().get(0).id();

        Task updated = adapter.toggleSubtask(created.id(), subtaskAId);

        assertThat(updated.subtasks().get(0).done()).isTrue();
        assertThat(updated.subtasks().get(1).done()).isFalse();
    }

    @Test
    void delete_removes_the_task() {
        Task created = adapter.create(new CreateTaskCommand(spaceId, "T", TaskPriority.LOW, null, List.of(), List.of(), null, null));

        adapter.delete(created.id());

        assertThat(adapter.findById(created.id())).isEmpty();
    }

    @Test
    void findBySpaceId_returns_every_task_in_the_space() {
        adapter.create(new CreateTaskCommand(spaceId, "T1", TaskPriority.LOW, null, List.of(), List.of(), null, null));
        adapter.create(new CreateTaskCommand(spaceId, "T2", TaskPriority.LOW, null, List.of(), List.of(), null, null));

        assertThat(adapter.findBySpaceId(spaceId)).hasSize(2);
    }

    @Test
    void findOpenBySpaceId_returns_the_todo_and_doing_tasks_of_the_space_but_not_the_done_ones() {
        Task todo = adapter.create(new CreateTaskCommand(spaceId, "À faire", TaskPriority.LOW, null, List.of(), List.of(), null, null));
        Task doing = adapter.create(new CreateTaskCommand(spaceId, "En cours", TaskPriority.LOW, LocalDate.of(2026, 1, 7), List.of(), List.of(), null, null));
        adapter.updateStatus(doing.id(), TaskStatus.DOING);
        Task done = adapter.create(new CreateTaskCommand(spaceId, "Fait", TaskPriority.LOW, null, List.of(), List.of(), null, null));
        adapter.updateStatus(done.id(), TaskStatus.DONE);

        SpaceEntity other = new SpaceEntity();
        other.setType(SpaceType.SHARED);
        TestSpaces.name(other, "Ailleurs");
        other.setAccent("#c17a5c");
        other.setGlyph("🏡");
        UUID otherSpaceId = spaceJpaRepository.saveAndFlush(other).getId();
        adapter.create(new CreateTaskCommand(otherSpaceId, "Pas ici", TaskPriority.LOW, null, List.of(), List.of(), null, null));

        assertThat(adapter.findOpenBySpaceId(spaceId))
            .extracting(Task::id)
            .containsExactlyInAnyOrder(todo.id(), doing.id());
    }

    @Test
    void createAll_persists_every_task_with_its_own_assignees_subtasks_and_creator_in_one_batch() {
        adapter.createAll(List.of(
            new CreateTaskCommand(spaceId, "T1", TaskPriority.LOW, LocalDate.of(2026, 1, 7),
                List.of(aliceId), List.of(new SubtaskInput("Vérifier le tri", false)), null, aliceId),
            new CreateTaskCommand(spaceId, "T2", TaskPriority.MED, LocalDate.of(2026, 1, 14), List.of(), List.of(), null, null)));

        List<Task> found = adapter.findBySpaceId(spaceId);
        assertThat(found).hasSize(2);
        Task t1 = found.stream().filter(t -> t.title().equals("T1")).findFirst().orElseThrow();
        assertThat(t1.assigneeIds()).containsExactly(aliceId);
        assertThat(t1.subtasks()).hasSize(1);
        assertThat(t1.subtasks().get(0).text()).isEqualTo("Vérifier le tri");
        assertThat(t1.createdBy()).isEqualTo(aliceId);
        Task t2 = found.stream().filter(t -> t.title().equals("T2")).findFirst().orElseThrow();
        assertThat(t2.assigneeIds()).isEmpty();
        assertThat(t2.subtasks()).isEmpty();
        assertThat(t2.createdBy()).isNull();
    }

    @Test
    void createAll_does_nothing_for_an_empty_list() {
        adapter.createAll(List.of());

        assertThat(adapter.findBySpaceId(spaceId)).isEmpty();
    }

    @Test
    void the_title_and_the_subtasks_are_stored_sealed_with_the_key_of_the_space() {
        Task created = adapter.create(new CreateTaskCommand(spaceId, "Rendez-vous oncologue", TaskPriority.HIGH, null,
            List.of(), List.of(new SubtaskInput("Apporter l’ordonnance", false)), null, aliceId));

        String title = jdbc.queryForObject("SELECT title_encrypted FROM tasks WHERE id = ?", String.class, created.id());
        assertThat(title).startsWith("v3:").doesNotContain("oncologue");
        assertThat(sealers.forSpace(spaceId).open(TaskEntity.TITLE, created.id(), title)).isEqualTo("Rendez-vous oncologue");
        assertThat(storedSubtaskTexts(created.id())).containsExactly("Apporter l’ordonnance");
    }

    @Test
    void editing_the_subtasks_seals_the_new_texts() {
        Task created = adapter.create(new CreateTaskCommand(spaceId, "Ménage", TaskPriority.LOW, null, List.of(),
            List.of(new SubtaskInput("Cuisine", false)), null, aliceId));
        UUID kitchen = created.subtasks().getFirst().id();

        Task updated = adapter.update(new UpdateTaskCommand(created.id(), spaceId, "Ménage", TaskPriority.LOW, null, List.of(),
            List.of(new SubtaskEdit(kitchen, "Cuisine et four"), new SubtaskEdit(null, "Salle de bain 🛁"))));

        assertThat(updated.subtasks()).extracting(Subtask::text).containsExactly("Cuisine et four", "Salle de bain 🛁");
        assertThat(storedSubtaskTexts(created.id())).containsExactly("Cuisine et four", "Salle de bain 🛁");
    }

    @Test
    void an_unchanged_subtask_keeps_its_stored_value() {
        Task created = adapter.create(new CreateTaskCommand(spaceId, "Ménage", TaskPriority.LOW, null, List.of(),
            List.of(new SubtaskInput("Cuisine", false)), null, aliceId));
        UUID kitchen = created.subtasks().getFirst().id();
        String before = jdbc.queryForObject("SELECT text_encrypted FROM task_subtasks WHERE id = ?", String.class, kitchen);

        adapter.update(new UpdateTaskCommand(created.id(), spaceId, "Grand ménage", TaskPriority.LOW, null, List.of(),
            List.of(new SubtaskEdit(kitchen, "Cuisine"))));

        assertThat(jdbc.queryForObject("SELECT text_encrypted FROM task_subtasks WHERE id = ?", String.class, kitchen)).isEqualTo(before);
    }

    @Test
    void a_title_or_a_subtask_copied_from_another_row_is_refused() {
        Task first = adapter.create(new CreateTaskCommand(spaceId, "Payer la cantine", TaskPriority.LOW, null, List.of(),
            List.of(new SubtaskInput("Trouver le RIB", false), new SubtaskInput("Virement", false)), null, aliceId));
        Task second = adapter.create(new CreateTaskCommand(spaceId, "Arroser", TaskPriority.LOW, null, List.of(), List.of(), null, aliceId));
        jdbc.update("UPDATE tasks SET title_encrypted = (SELECT title_encrypted FROM tasks WHERE id = ?) WHERE id = ?",
            first.id(), second.id());
        jdbc.update("UPDATE task_subtasks SET text_encrypted = (SELECT text_encrypted FROM task_subtasks WHERE id = ?) WHERE id = ?",
            first.subtasks().get(0).id(), first.subtasks().get(1).id());

        assertThatThrownBy(() -> adapter.findById(second.id())).isInstanceOf(SealedValueRejected.class);
        assertThatThrownBy(() -> adapter.findById(first.id())).isInstanceOf(SealedValueRejected.class);
    }

    private List<String> storedSubtaskTexts(UUID taskId) {
        SpaceSealer sealer = sealers.forSpace(spaceId);
        return jdbc.query("SELECT id, text_encrypted FROM task_subtasks WHERE task_id = ? ORDER BY position",
            (rs, rowNum) -> sealer.open(TaskSubtaskEntity.TEXT, rs.getObject("id", UUID.class), rs.getString("text_encrypted")), taskId);
    }

    @Test
    void an_update_seals_under_the_space_of_the_task_whatever_the_command_says() {
        Task created = adapter.create(new CreateTaskCommand(spaceId, "Ménage", TaskPriority.LOW, null, List.of(),
            List.of(new SubtaskInput("Cuisine", false)), null, aliceId));

        adapter.update(new UpdateTaskCommand(created.id(), anotherSpace(), "Grand ménage", TaskPriority.LOW, null, List.of(),
            List.of(new SubtaskEdit(created.subtasks().getFirst().id(), "Cuisine et four"), new SubtaskEdit(null, "Salle de bain"))));

        Task reread = adapter.findById(created.id()).orElseThrow();
        assertThat(reread.title()).isEqualTo("Grand ménage");
        assertThat(reread.subtasks()).extracting(Subtask::text).containsExactly("Cuisine et four", "Salle de bain");
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
