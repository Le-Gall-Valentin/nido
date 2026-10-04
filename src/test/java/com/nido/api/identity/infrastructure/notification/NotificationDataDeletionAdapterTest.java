package com.nido.api.identity.infrastructure.notification;

import com.nido.api.notifications.application.port.in.DeleteNotificationPreferencesUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationDataDeletionAdapterTest {

    @Mock DeleteNotificationPreferencesUseCase deletePreferences;

    @Test
    void the_account_choices_are_forgotten_by_the_notifications_context() {
        UUID userId = UUID.randomUUID();

        new NotificationDataDeletionAdapter(deletePreferences).deleteNotificationData(userId);

        verify(deletePreferences).deleteAllFor(userId);
    }
}
