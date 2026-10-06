package com.nido.api.identity.infrastructure.notification;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;
import com.nido.api.shared.model.Role;

/** mail/identity/activity-account-reactivated.html */
@NotificationKind(value = "identity.account-reactivated", roles = Role.SUPER_ADMIN)
public record AccountReactivatedNotification(String username, String actorName, String accountName, AppPath usersPath)
    implements Notification, MailContent {
    @Override
    public String template() {
        return "identity/activity-account-reactivated";
    }
}
