package com.nido.api.notifications.infrastructure.catalog;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.nido.api.mail.application.port.in.MailAvailabilityQuery;
import com.nido.api.mail.application.port.in.SendMailUseCase;
import com.nido.api.notifications.domain.model.NotificationCatalog;
import com.nido.api.notifications.domain.model.NotificationType;
import com.nido.api.notifications.domain.port.out.NotificationChannelPort;
import com.nido.api.notifications.infrastructure.channel.mail.MailChannelAdapter;
import com.nido.api.shared.model.Role;
import fixtures.notifications.valid.GreetingNotification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ScannedNotificationCatalogAdapterTest {

    private final List<NotificationChannelPort> mailOnly = List.of(
        new MailChannelAdapter(mock(SendMailUseCase.class), mock(MailAvailabilityQuery.class)));

    private final ListAppender<ILoggingEvent> logged = new ListAppender<>();

    @BeforeEach
    void listen() {
        logged.start();
        ((Logger) LoggerFactory.getLogger(ScannedNotificationCatalogAdapter.class)).addAppender(logged);
    }

    @AfterEach
    void stopListening() {
        ((Logger) LoggerFactory.getLogger(ScannedNotificationCatalogAdapter.class)).detachAppender(logged);
    }

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
    void a_kind_declared_for_some_roles_is_open_to_those_roles_only() {
        NotificationCatalog catalog = scan("fixtures.notifications.reserved");
        NotificationType warden = new NotificationType("fixture.warden");

        assertThat(catalog.isOpenTo(warden, Role.SUPER_ADMIN)).isTrue();
        assertThat(catalog.isOpenTo(warden, Role.ADMIN)).isFalse();
        assertThat(catalog.typesFor(Role.USER)).isEmpty();
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
    void finding_no_notification_at_all_stops_the_start() {
        // What a packaging the scan cannot read would look like: the application always declares some.
        assertThatThrownBy(() -> scan("fixtures.notifications.none"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No notification found")
            .hasMessageContaining("fixtures.notifications.none");
    }

    @Test
    void the_kinds_found_are_logged_at_startup() {
        scan("fixtures.notifications.valid");

        assertThat(logged.list).extracting(ILoggingEvent::getFormattedMessage)
            .containsExactly("Notification kinds (2): [agenda.reminder, fixture.greeting]");
    }

    @Test
    void the_application_and_its_tests_declare_nothing_it_cannot_send() {
        // Scans test classes too, as an integration test's startup does: a test notification left under
        // com.nido.api would fail here first, with its name.
        assertThatCode(() -> scan("com.nido.api")).doesNotThrowAnyException();
    }
}
