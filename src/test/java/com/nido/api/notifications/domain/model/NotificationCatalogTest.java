package com.nido.api.notifications.domain.model;

import com.nido.api.shared.model.Role;
import fixtures.notifications.valid.GreetingNotification;
import fixtures.notifications.valid.ReminderNotification;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationCatalogTest {

    private static final NotificationType GREETING = new NotificationType("fixture.greeting");
    private static final NotificationType REMINDER = new NotificationType("agenda.reminder");

    private final NotificationCatalog catalog = new NotificationCatalog(Map.of(
        GreetingNotification.class, GREETING,
        ReminderNotification.class, REMINDER));

    @Test
    void each_class_has_its_kind() {
        assertThat(catalog.typeOf(GreetingNotification.class)).isEqualTo(GREETING);
        assertThat(catalog.typeOf(ReminderNotification.class)).isEqualTo(REMINDER);
    }

    @Test
    void kinds_are_listed_by_context_then_by_code() {
        assertThat(catalog.types()).containsExactly(REMINDER, GREETING);
    }

    @Test
    void a_kind_is_found_by_its_code() {
        assertThat(catalog.find("fixture.greeting")).contains(GREETING);
        assertThat(catalog.find("fixture.unknown")).isEmpty();
        assertThat(catalog.find("Not A Code")).isEmpty();
        assertThat(catalog.find(null)).isEmpty();
    }

    @Test
    void a_class_it_does_not_know_is_a_bug() {
        NotificationCatalog empty = new NotificationCatalog(Map.of());

        assertThatThrownBy(() -> empty.typeOf(GreetingNotification.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(GreetingNotification.class.getName());
    }

    @Test
    void a_kind_without_roles_is_open_to_everyone() {
        assertThat(catalog.isOpenTo(GREETING, Role.USER)).isTrue();
        assertThat(catalog.typesFor(Role.USER)).containsExactly(REMINDER, GREETING);
    }

    @Test
    void a_reserved_kind_is_open_to_its_roles_only_and_listed_for_them_alone() {
        NotificationCatalog reserved = new NotificationCatalog(
            Map.of(GreetingNotification.class, GREETING, ReminderNotification.class, REMINDER),
            Map.of(GREETING, Set.of(Role.SUPER_ADMIN)));

        assertThat(reserved.isOpenTo(GREETING, Role.SUPER_ADMIN)).isTrue();
        assertThat(reserved.isOpenTo(GREETING, Role.ADMIN)).isFalse();
        assertThat(reserved.typesFor(Role.USER)).containsExactly(REMINDER);
        assertThat(reserved.typesFor(Role.SUPER_ADMIN)).containsExactly(REMINDER, GREETING);
        assertThat(reserved.types()).containsExactly(REMINDER, GREETING);
    }

    @Test
    void two_classes_cannot_share_a_kind() {
        assertThatThrownBy(() -> new NotificationCatalog(Map.of(
                GreetingNotification.class, GREETING,
                ReminderNotification.class, GREETING)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("fixture.greeting");
    }
}
