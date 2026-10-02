package com.nido.api.notifications.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTypeTest {

    @Test
    void a_kind_reads_context_dot_name_and_belongs_to_its_context() {
        NotificationType type = new NotificationType("space.invitation");

        assertThat(type.code()).isEqualTo("space.invitation");
        assertThat(type.group()).isEqualTo("space");
    }

    @Test
    void digits_and_hyphens_are_allowed_after_the_first_letter() {
        assertThatCode(() -> new NotificationType("tasks2.due-soon")).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "space", "Space.invitation", "space.Invitation", "space.invitation.extra",
        ".invitation", "space.", "space invitation", "1space.invitation", "space.-invitation"})
    void anything_else_is_refused(String code) {
        assertThatThrownBy(() -> new NotificationType(code)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void no_code_is_refused() {
        assertThatThrownBy(() -> new NotificationType(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void a_code_wider_than_its_column_is_refused() {
        assertThatThrownBy(() -> new NotificationType("a." + "b".repeat(63)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> new NotificationType("a." + "b".repeat(62))).doesNotThrowAnyException();
    }
}
