package com.nido.api.notifications.application.handler;

import com.nido.api.notifications.domain.model.NotificationCatalog;
import com.nido.api.notifications.domain.model.NotificationRequest;
import com.nido.api.notifications.domain.model.NotificationType;
import com.nido.api.notifications.domain.port.out.NotificationChannelPort;
import com.nido.api.notifications.domain.port.out.NotificationDeliveryPort;
import fixtures.notifications.valid.GreetingNotification;
import fixtures.notifications.valid.ReminderNotification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifyHandlerTest {

    private static final NotificationType GREETING = new NotificationType("fixture.greeting");
    private static final NotificationCatalog CATALOG = new NotificationCatalog(Map.of(GreetingNotification.class, GREETING));

    @Mock NotificationChannelPort mail;
    @Mock NotificationDeliveryPort delivery;

    private final NotificationRequest request =
        new NotificationRequest(UUID.randomUUID(), new GreetingNotification("jane"), null);

    private NotifyHandler handler() {
        return new NotifyHandler(() -> CATALOG, List.of(mail), delivery);
    }

    @Test
    void the_kind_is_read_now_and_the_delivery_waits_for_the_commit() {
        when(mail.canDeliver(GreetingNotification.class)).thenReturn(true);

        handler().notify(request);

        verify(delivery).deliverAfterCommit(GREETING, request);
    }

    @Test
    void without_a_channel_that_can_deliver_it_nothing_waits_for_the_commit() {
        when(mail.canDeliver(GreetingNotification.class)).thenReturn(false);

        handler().notify(request);

        verifyNoInteractions(delivery);
    }

    @Test
    void an_uncatalogued_notification_is_a_bug_not_a_silence() {
        NotificationRequest reminder = new NotificationRequest(UUID.randomUUID(), new ReminderNotification("x"), null);

        assertThatThrownBy(() -> handler().notify(reminder))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(ReminderNotification.class.getName());
        verifyNoInteractions(delivery);
    }
}
