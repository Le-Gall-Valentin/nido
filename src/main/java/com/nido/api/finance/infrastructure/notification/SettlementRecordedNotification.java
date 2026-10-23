package com.nido.api.finance.infrastructure.notification;

import com.nido.api.finance.domain.model.SettlementAuthor;
import com.nido.api.finance.domain.model.Standing;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * mail/finance/settlement-recorded.html — told to the other side of a repayment, never to whoever recorded it, with
 * what is left between the two of them.
 *
 * @param author    which side recorded it: the one who paid back, or the one paid back
 * @param remaining what is still owed between them, from the reader's side; zero when they are even
 */
@NotificationKind("finance.settlement-recorded")
public record SettlementRecordedNotification(String username, String actorName, String spaceName, SettlementAuthor author,
                                             BigDecimal amount, LocalDate date, Standing standing, BigDecimal remaining,
                                             AppPath financePath)
    implements Notification, MailContent {

    @Override
    public String template() {
        return "finance/settlement-recorded";
    }
}
