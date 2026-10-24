package com.nido.api.finance.infrastructure.notification;

import com.nido.api.finance.domain.model.OperationFacts;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;

import java.util.UUID;

/** mail/finance/transaction-deleted.html — told to whoever a deleted operation concerned, not its author. */
@NotificationKind("finance.transaction-deleted")
public record TransactionDeletedNotification(String username, UUID readerId, String actorName, String spaceName, OperationFacts operation, AppPath financePath)
    implements Notification, MailContent {

    @Override
    public String template() {
        return "finance/transaction-deleted";
    }
}
