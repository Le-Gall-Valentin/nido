package com.nido.api.identity.infrastructure.notification;

import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.port.out.AdminActivityNotificationPort;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.notifications.application.port.in.NotifyUseCase;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The super-administrators' notifications, sent through the notifications context: one request per reader,
 * so each mail greets the person it goes to and is written in their language. Without expiry: what an
 * administrator did stays worth knowing.
 */
@Component
public class AdminActivityNotificationAdapter implements AdminActivityNotificationPort {

    private static final AppPath USERS = new AppPath("/administration/users");

    private final NotifyUseCase notify;

    public AdminActivityNotificationAdapter(NotifyUseCase notify) {
        this.notify = notify;
    }

    @Override
    public void accountCreated(List<User> readers, String actorName, String accountName) {
        readers.forEach(reader -> tell(reader, new AccountCreatedNotification(reader.username(), actorName, accountName, USERS)));
    }

    @Override
    public void accountDeactivated(List<User> readers, String actorName, String accountName) {
        readers.forEach(reader -> tell(reader, new AccountDeactivatedNotification(reader.username(), actorName, accountName, USERS)));
    }

    @Override
    public void accountReactivated(List<User> readers, String actorName, String accountName) {
        readers.forEach(reader -> tell(reader, new AccountReactivatedNotification(reader.username(), actorName, accountName, USERS)));
    }

    @Override
    public void accountDeleted(List<User> readers, String actorName, String accountName, boolean wasInvited) {
        readers.forEach(reader ->
            tell(reader, new AccountDeletedNotification(reader.username(), actorName, accountName, wasInvited, USERS)));
    }

    @Override
    public void totpReset(List<User> readers, String actorName, String accountName) {
        readers.forEach(reader -> tell(reader, new AccountTotpResetNotification(reader.username(), actorName, accountName, USERS)));
    }

    private void tell(User reader, Notification notification) {
        notify.notify(new NotificationRequest(reader.id(), notification, null));
    }
}
