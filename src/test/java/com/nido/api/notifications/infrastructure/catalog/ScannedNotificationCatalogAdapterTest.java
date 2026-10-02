package com.nido.api.notifications.infrastructure.catalog;

import com.nido.api.mail.application.port.in.MailAvailabilityQuery;
import com.nido.api.mail.application.port.in.SendMailUseCase;
import com.nido.api.notifications.domain.model.NotificationCatalog;
import com.nido.api.notifications.domain.model.NotificationType;
import com.nido.api.notifications.domain.port.out.NotificationChannelPort;
import com.nido.api.notifications.infrastructure.channel.mail.MailChannelAdapter;
import fixtures.notifications.valid.GreetingNotification;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ScannedNotificationCatalogAdapterTest {

    private final List<NotificationChannelPort> mailOnly = List.of(
        new MailChannelAdapter(mock(SendMailUseCase.class), mock(MailAvailabilityQuery.class)));

    private NotificationCatalog scan(String basePackage) {
        return new ScannedNotificationCatalogAdapter(List.of(basePackage), mailOnly).catalog();
    }

    @Test
    void every_declared_notification_is_found_with_its_kind() {
        NotificationCatalog catalog = scan("fixtures.notifications.valid");

        assertThat(catalog.types()).extracting(NotificationType::code)
            .containsExactly("agenda.reminder", "fixture.greeting");
        assertThat(catalog.typeOf(GreetingNotification.class)).isEqualTo(new NotificationType("fixture.greeting"));
    }

    @Test
    void a_notification_that_forgets_its_kind_stops_the_start() {
        assertThatThrownBy(() -> scan("fixtures.notifications.unannotated"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("UnannotatedNotification")
            .hasMessageContaining("@NotificationKind");
    }

    @Test
    void two_notifications_of_one_kind_stop_the_start() {
        assertThatThrownBy(() -> scan("fixtures.notifications.duplicate"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("fixture.twin");
    }

    @Test
    void a_malformed_kind_stops_the_start_and_names_its_class() {
        assertThatThrownBy(() -> scan("fixtures.notifications.malformed"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("MalformedNotification");
    }

    @Test
    void a_notification_no_channel_can_write_stops_the_start() {
        assertThatThrownBy(() -> scan("fixtures.notifications.nochannel"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NoChannelNotification");
    }

    @Test
    void the_application_and_its_tests_declare_nothing_it_cannot_send() {
        // Scans test classes too, as an integration test's startup does: a test notification left under
        // com.nido.api would fail here first, with its name.
        assertThatCode(() -> scan("com.nido.api")).doesNotThrowAnyException();
    }
}
