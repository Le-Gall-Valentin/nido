package com.nido.api.identity.infrastructure.notification;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;
import com.nido.api.shared.model.Role;

/** mail/identity/activity-account-created.html — told to super-administrators when an administrator invites someone. */
@NotificationKind(value = "identity.account-created", roles = Role.SUPER_ADMIN)
public record AccountCreatedNotification(String username, String actorName, String accountName, AppPath usersPath)
    implements Notification, MailContent {
    @Override
    public String template() {
        return "identity/activity-account-created";
    }
}
