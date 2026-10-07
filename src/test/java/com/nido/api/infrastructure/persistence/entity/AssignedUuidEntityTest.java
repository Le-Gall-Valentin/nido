package com.nido.api.infrastructure.persistence.entity;

import com.nido.api.tasks.infrastructure.persistence.entity.TaskEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class AssignedUuidEntityTest {

    @Test
    void building_an_entity_the_way_hibernate_does_to_load_a_row_draws_no_id() throws Exception {
        // Hibernate builds every row it loads through the constructor, then sets the id the row has.
        Field id = AssignedUuidEntity.class.getDeclaredField("id");
        id.setAccessible(true);

        assertThat(id.get(new TaskEntity())).isNull();
    }

    @Test
    void a_new_entity_keeps_the_id_it_was_first_given() {
        TaskEntity task = new TaskEntity();

        assertThat(task.getId()).isNotNull().isEqualTo(task.getId());
        assertThat(new TaskEntity().getId()).isNotEqualTo(task.getId());
    }
}
