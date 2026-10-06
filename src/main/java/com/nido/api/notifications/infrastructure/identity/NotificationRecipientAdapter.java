package com.nido.api.notifications.infrastructure.identity;

import com.nido.api.identity.application.port.in.FindUserUseCase;
import com.nido.api.notifications.domain.model.NotificationRecipient;
import com.nido.api.notifications.domain.port.out.NotificationRecipientPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Who a notification is for, as identity knows them. A deleted account is never found (FindUserUseCase#findById). */
@Component
public class NotificationRecipientAdapter implements NotificationRecipientPort {

    private final FindUserUseCase findUser;

    public NotificationRecipientAdapter(FindUserUseCase findUser) {
        this.findUser = findUser;
    }

    @Override
    public Optional<NotificationRecipient> find(UUID userId) {
        return findUser.findById(userId).map(user ->
            new NotificationRecipient(user.id(), user.username(), user.email(), user.language(), user.role(), user.isActive()));
    }
}
