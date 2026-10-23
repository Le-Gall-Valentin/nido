package com.nido.api.finance.infrastructure.notification;

import com.nido.api.finance.domain.model.ContributorSide;
import com.nido.api.finance.domain.model.SavingsDeposit;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;

/**
 * mail/finance/savings-goal-reached.html — told to every member but its author when a deposit brings a goal to
 * its target; whoever switched it off gets the plain deposit mail instead.
 */
@NotificationKind("finance.savings-goal-reached")
public record SavingsGoalReachedNotification(String username, String actorName, String spaceName, ContributorSide side,
                                             SavingsDeposit deposit, AppPath financePath)
    implements Notification, MailContent {

    @Override
    public String template() {
        return "finance/savings-goal-reached";
    }
}
