package com.nido.api.finance.infrastructure.notification;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * mail/finance/opening-balance-changed.html — told to every member but its author when the opening balance of a space
 * is set or removed.
 *
 * @param amount the balance set, with {@code asOf} its day; both null when it was removed
 */
@NotificationKind("finance.opening-balance-changed")
public record OpeningBalanceChangedNotification(String username, String actorName, String spaceName, boolean cleared,
                                                BigDecimal amount, LocalDate asOf, AppPath financePath)
    implements Notification, MailContent {

    @Override
    public String template() {
        return "finance/opening-balance-changed";
    }
}
