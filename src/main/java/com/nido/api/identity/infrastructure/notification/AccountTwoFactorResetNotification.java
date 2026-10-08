package com.nido.api.identity.infrastructure.notification;

import com.nido.api.identity.infrastructure.mail.ResetMethods;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;
import com.nido.api.shared.model.Role;

/** mail/identity/activity-account-2fa-reset.html — the kind keeps its name: preferences already saved stay as they are. */
@NotificationKind(value = "identity.account-2fa-reset", roles = Role.SUPER_ADMIN)
public record AccountTwoFactorResetNotification(String username, String actorName, String accountName,
                                                ResetMethods removed, AppPath usersPath)
    implements Notification, MailContent {
    @Override
    public String template() {
        return "identity/activity-account-2fa-reset";
    }
}
