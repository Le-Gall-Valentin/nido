package com.nido.api.finance.infrastructure.notification;

import com.nido.api.finance.domain.model.GoalFacts;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;

/**
 * mail/finance/savings-goal-updated.html — told to every member but its author when a goal's name, target or date
 * changes.
 */
@NotificationKind("finance.savings-goal-updated")
public record SavingsGoalUpdatedNotification(String username, String actorName, String spaceName, GoalFacts before, GoalFacts after,
                                             AppPath financePath)
    implements Notification, MailContent {

    @Override
    public String template() {
        return "finance/savings-goal-updated";
    }
}
