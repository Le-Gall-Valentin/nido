package com.nido.api.identity.infrastructure.notification;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;
import com.nido.api.shared.model.Role;

/** mail/identity/activity-account-deleted.html — the deleted account named, read before the anonymisation. */
@NotificationKind(value = "identity.account-deleted", roles = Role.SUPER_ADMIN)
public record AccountDeletedNotification(String username, String actorName, String accountName, AppPath usersPath)
    implements Notification, MailContent {
    @Override
    public String template() {
        return "identity/activity-account-deleted";
    }
}
