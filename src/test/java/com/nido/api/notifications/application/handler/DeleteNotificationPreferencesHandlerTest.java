package com.nido.api.notifications.application.handler;

import com.nido.api.notifications.domain.port.out.NotificationPreferencesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeleteNotificationPreferencesHandlerTest {

    @Mock NotificationPreferencesRepository preferences;

    @Test
    void every_choice_of_the_account_is_forgotten() {
        UUID janeId = UUID.randomUUID();

        new DeleteNotificationPreferencesHandler(preferences).deleteAllFor(janeId);

        verify(preferences).deleteAllFor(janeId);
    }
}
