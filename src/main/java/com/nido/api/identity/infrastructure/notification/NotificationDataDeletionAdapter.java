package com.nido.api.identity.infrastructure.notification;

import com.nido.api.identity.domain.port.out.NotificationDataDeletionPort;
import com.nido.api.notifications.application.port.in.DeleteNotificationPreferencesUseCase;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NotificationDataDeletionAdapter implements NotificationDataDeletionPort {

    private final DeleteNotificationPreferencesUseCase deletePreferences;

    public NotificationDataDeletionAdapter(DeleteNotificationPreferencesUseCase deletePreferences) {
        this.deletePreferences = deletePreferences;
    }

    @Override
    public void deleteNotificationData(UUID userId) {
        deletePreferences.deleteAllFor(userId);
    }
}
