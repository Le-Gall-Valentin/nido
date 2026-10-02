package com.nido.api.notifications.application.handler;

import com.nido.api.notifications.application.port.in.DeleteNotificationPreferencesUseCase;
import com.nido.api.notifications.domain.port.out.NotificationPreferencesRepository;
import com.nido.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class DeleteNotificationPreferencesHandler implements DeleteNotificationPreferencesUseCase {

    private final NotificationPreferencesRepository preferences;

    public DeleteNotificationPreferencesHandler(NotificationPreferencesRepository preferences) {
        this.preferences = preferences;
    }

    @Override
    @Transactional
    public void deleteAllFor(UUID userId) {
        preferences.deleteAllFor(userId);
    }
}
