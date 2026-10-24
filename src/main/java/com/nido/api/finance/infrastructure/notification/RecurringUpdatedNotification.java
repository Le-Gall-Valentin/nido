package com.nido.api.finance.infrastructure.notification;

import com.nido.api.finance.domain.model.Involvement;
import com.nido.api.finance.domain.model.SeriesFacts;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;

import java.util.UUID;

/**
 * mail/finance/recurring-updated.html — told to whoever a changed recurring transaction concerned before or concerns
 * after, not its author; {@code involvement} says which of the three it is to them.
 */
@NotificationKind("finance.recurring-updated")
public record RecurringUpdatedNotification(String username, UUID readerId, String actorName, String spaceName, Involvement involvement,
                                           SeriesFacts before, SeriesFacts after, AppPath financePath)
    implements Notification, MailContent {

    @Override
    public String template() {
        return "finance/recurring-updated";
    }

    /** The label the reader knows it by: the one it had, for whoever is no longer part of it. */
    public String label() {
        return involvement == Involvement.REMOVED ? before.label() : after.label();
    }
}
