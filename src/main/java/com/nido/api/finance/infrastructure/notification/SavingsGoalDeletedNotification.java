package com.nido.api.finance.infrastructure.notification;

import com.nido.api.finance.domain.model.GoalFacts;
import com.nido.api.finance.domain.model.MemberShare;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;

import java.math.BigDecimal;
import java.util.List;

/**
 * mail/finance/savings-goal-deleted.html — told to every member but its author when a goal is deleted, with what
 * each member had saved on it: the deposits go with it.
 */
@NotificationKind("finance.savings-goal-deleted")
public record SavingsGoalDeletedNotification(String username, String actorName, String spaceName, GoalFacts goal, BigDecimal saved,
                                             List<MemberShare> savedByMember, AppPath financePath)
    implements Notification, MailContent {

    @Override
    public String template() {
        return "finance/savings-goal-deleted";
    }
}
