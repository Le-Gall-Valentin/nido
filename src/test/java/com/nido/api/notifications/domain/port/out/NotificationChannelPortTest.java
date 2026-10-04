package com.nido.api.notifications.domain.port.out;

import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationChannel;
import com.nido.api.notifications.domain.model.NotificationRecipient;
import com.nido.api.notifications.domain.model.NotificationType;
import fixtures.notifications.valid.GreetingNotification;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationChannelPortTest {

    private static NotificationChannelPort channel(boolean available, boolean supports) {
        return new NotificationChannelPort() {
            @Override public NotificationChannel channel() { return NotificationChannel.EMAIL; }
            @Override public boolean isAvailable() { return available; }
            @Override public boolean supports(Class<? extends Notification> notificationClass) { return supports; }
            @Override public void deliver(NotificationRecipient recipient, NotificationType type,
                                          Notification notification, Instant expiresAt) { }
        };
    }

    @Test
    void it_can_deliver_only_what_it_writes_and_only_when_this_installation_has_it() {
        assertThat(channel(true, true).canDeliver(GreetingNotification.class)).isTrue();
        assertThat(channel(false, true).canDeliver(GreetingNotification.class)).isFalse();
        assertThat(channel(true, false).canDeliver(GreetingNotification.class)).isFalse();
    }
}
