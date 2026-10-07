package com.nido.api.infrastructure.persistence.entity;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.TestSpaces;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import com.nido.api.tasks.domain.model.TaskPriority;
import com.nido.api.tasks.domain.model.TaskStatus;
import com.nido.api.tasks.infrastructure.persistence.entity.TaskEntity;
import com.nido.api.tasks.infrastructure.persistence.repository.TaskJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class AssignedUuidEntityIT {

    @Autowired TaskJpaRepository tasks;
    @Autowired SpaceJpaRepository spaces;
    @Autowired JdbcTemplate jdbc;

    private TaskEntity newTask() {
        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        TestSpaces.name(space, "Ids");
        space.setAccent("#c17a5c");
        space.setGlyph("🏡");
        TaskEntity task = new TaskEntity();
        task.setSpaceId(spaces.saveAndFlush(space).getId());
        task.setTitleEncrypted("unread");
        task.setStatus(TaskStatus.TODO);
        task.setPriority(TaskPriority.LOW);
        return task;
    }

    @Test
    void a_new_entity_is_inserted_under_the_id_it_was_built_with() {
        TaskEntity task = newTask();
        UUID built = task.getId();

        assertThat(built).isNotNull();
        assertThat(tasks.saveAndFlush(task).getId()).isEqualTo(built);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tasks WHERE id = ?", Long.class, built)).isEqualTo(1);
    }

    @Test
    void an_entity_saved_before_anyone_asked_its_id_is_given_one_as_it_is_inserted() {
        TaskEntity task = newTask();

        UUID id = tasks.saveAndFlush(task).getId();

        assertThat(id).isNotNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tasks WHERE id = ?", Long.class, id)).isEqualTo(1);
    }

    @Test
    void a_loaded_entity_is_updated_rather_than_inserted_again() {
        UUID id = tasks.saveAndFlush(newTask()).getId();

        TaskEntity loaded = tasks.findById(id).orElseThrow();
        assertThat(loaded.isNew()).isFalse();
        loaded.setPriority(TaskPriority.HIGH);
        tasks.saveAndFlush(loaded);

        assertThat(jdbc.queryForObject("SELECT priority FROM tasks WHERE id = ?", String.class, id)).isEqualTo("HIGH");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tasks WHERE id = ?", Long.class, id)).isEqualTo(1);
    }
}
