package com.nido.api.finance.infrastructure.notification;

import com.nido.api.finance.domain.model.SeriesFacts;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;

import java.util.UUID;

/** mail/finance/recurring-created.html — told to whoever a new recurring transaction concerns, not its author. */
@NotificationKind("finance.recurring-created")
public record RecurringCreatedNotification(String username, UUID readerId, String actorName, String spaceName, SeriesFacts series, AppPath financePath)
    implements Notification, MailContent {

    @Override
    public String template() {
        return "finance/recurring-created";
    }
}
