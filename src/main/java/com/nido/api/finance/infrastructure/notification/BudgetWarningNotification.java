package com.nido.api.finance.infrastructure.notification;

import com.nido.api.finance.domain.model.BudgetAlert;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;

/**
 * mail/finance/budget-warning.html — told to every member but its author when an operation makes a category's
 * spending this month crosses 80 % of its monthly budget.
 */
@NotificationKind("finance.budget-warning")
public record BudgetWarningNotification(String username, String actorName, String spaceName, BudgetAlert alert,
                                        AppPath financePath)
    implements Notification, MailContent {

    @Override
    public String template() {
        return "finance/budget-warning";
    }
}
