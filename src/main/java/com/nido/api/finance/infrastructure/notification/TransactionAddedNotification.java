package com.nido.api.finance.infrastructure.notification;

import com.nido.api.finance.domain.model.OperationFacts;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;

import java.util.UUID;

/** mail/finance/transaction-added.html — told to whoever an added operation concerns: its payer and its contributors, not its author. */
@NotificationKind("finance.transaction-added")
public record TransactionAddedNotification(String username, UUID readerId, String actorName, String spaceName, OperationFacts operation, AppPath financePath)
    implements Notification, MailContent {

    @Override
    public String template() {
        return "finance/transaction-added";
    }
}
