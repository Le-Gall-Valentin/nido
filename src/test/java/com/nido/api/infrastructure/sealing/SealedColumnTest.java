package com.nido.api.infrastructure.sealing;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SealedColumnTest {

    @Test
    void a_reference_names_table_column_and_row() {
        UUID id = UUID.fromString("6f1d7c4e-0000-4000-8000-000000000001");

        assertThat(SealedColumn.ofSpace("tasks", "title_encrypted").referenceFor(id))
            .isEqualTo("tasks.title_encrypted:6f1d7c4e-0000-4000-8000-000000000001");
    }

    @Test
    void the_space_of_a_row_is_found_by_the_kind_of_table() {
        assertThat(SealedColumn.ofSpace("tasks", "title_encrypted").spaceOf()).isEqualTo("t.space_id");
        SealedColumn child = SealedColumn.throughParent("task_subtasks", "text_encrypted", "task_id", "tasks");
        assertThat(child.spaceOf()).isEqualTo("p.space_id");
        assertThat(child.join()).isEqualTo("JOIN tasks p ON p.id = t.task_id");
        assertThat(SealedColumn.ofSpaceItself("spaces", "name_encrypted").spaceOf()).isEqualTo("t.id");
    }

    @Test
    void only_plain_identifiers_get_into_sql() {
        assertThatThrownBy(() -> SealedColumn.ofSpace("tasks; drop", "x")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SealedColumn.ofSpace("tasks", "title_encrypted").withClearColumn("Title"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(SealedColumn.ofSpace("tasks", "title_encrypted").withClearColumn("title").clearColumn()).contains("title");
    }
}
