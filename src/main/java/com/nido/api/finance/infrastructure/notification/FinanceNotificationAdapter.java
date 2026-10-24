package com.nido.api.finance.infrastructure.notification;

import com.nido.api.finance.domain.model.BudgetAlert;
import com.nido.api.finance.domain.model.BudgetStatus;
import com.nido.api.finance.domain.model.ContributorSide;
import com.nido.api.finance.domain.model.Gesture;
import com.nido.api.finance.domain.model.GoalFacts;
import com.nido.api.finance.domain.model.Involved;
import com.nido.api.finance.domain.model.MemberShare;
import com.nido.api.finance.domain.model.OperationFacts;
import com.nido.api.finance.domain.model.SavingsDeposit;
import com.nido.api.finance.domain.model.SeriesFacts;
import com.nido.api.finance.domain.model.SettlementAuthor;
import com.nido.api.finance.domain.model.Standing;
import com.nido.api.finance.domain.port.out.FinanceNotificationPort;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.notifications.application.port.in.NotifyUseCase;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationRequest;
import com.nido.api.space.domain.model.Addressee;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The finance context's notifications, sent through the notifications context: one request per reader, so each mail
 * greets the person it goes to, is written in their language and reads "you" where it is about them.
 */
@Component
public class FinanceNotificationAdapter implements FinanceNotificationPort {

    private final NotifyUseCase notify;

    public FinanceNotificationAdapter(NotifyUseCase notify) {
        this.notify = notify;
    }

    @Override
    public void transactionAdded(Gesture gesture, OperationFacts operation, List<Addressee> readers) {
        readers.forEach(reader -> tell(reader, new TransactionAddedNotification(reader.username(), reader.userId(),
            gesture.actorName(), gesture.spaceName(), operation, finance(gesture.spaceId()))));
    }

    @Override
    public void transactionUpdated(Gesture gesture, OperationFacts before, OperationFacts after, List<Involved> readers) {
        readers.forEach(involved -> tell(involved.reader(), new TransactionUpdatedNotification(involved.reader().username(),
            involved.reader().userId(), gesture.actorName(), gesture.spaceName(), involved.involvement(), before, after,
            finance(gesture.spaceId()))));
    }

    @Override
    public void transactionDeleted(Gesture gesture, OperationFacts operation, List<Addressee> readers) {
        readers.forEach(reader -> tell(reader, new TransactionDeletedNotification(reader.username(), reader.userId(),
            gesture.actorName(), gesture.spaceName(), operation, finance(gesture.spaceId()))));
    }

    @Override
    public void recurringCreated(Gesture gesture, SeriesFacts series, List<Addressee> readers) {
        readers.forEach(reader -> tell(reader, new RecurringCreatedNotification(reader.username(), reader.userId(),
            gesture.actorName(), gesture.spaceName(), series, finance(gesture.spaceId()))));
    }

    @Override
    public void recurringUpdated(Gesture gesture, SeriesFacts before, SeriesFacts after, List<Involved> readers) {
        readers.forEach(involved -> tell(involved.reader(), new RecurringUpdatedNotification(involved.reader().username(),
            involved.reader().userId(), gesture.actorName(), gesture.spaceName(), involved.involvement(), before, after,
            finance(gesture.spaceId()))));
    }

    @Override
    public void recurringStopped(Gesture gesture, SeriesFacts series, List<Addressee> readers) {
        readers.forEach(reader -> tell(reader, new RecurringStoppedNotification(reader.username(), reader.userId(),
            gesture.actorName(), gesture.spaceName(), series, finance(gesture.spaceId()))));
    }

    @Override
    public void savingsDeposited(Gesture gesture, SavingsDeposit deposit, List<Addressee> readers) {
        readers.forEach(reader -> {
            ContributorSide side = sideOf(deposit, gesture, reader);
            SavingsContributionNotification plain = new SavingsContributionNotification(reader.username(),
                gesture.actorName(), gesture.spaceName(), side, deposit, finance(gesture.spaceId()));
            if (deposit.reachesGoal()) {
                // Whoever switched "goal reached" off still hears of the deposit, as they would have of any other.
                notify.notify(new NotificationRequest(reader.userId(), new SavingsGoalReachedNotification(reader.username(),
                    gesture.actorName(), gesture.spaceName(), side, deposit, finance(gesture.spaceId())), null, plain));
            } else {
                tell(reader, plain);
            }
        });
    }

    @Override
    public void savingsGoalCreated(Gesture gesture, GoalFacts goal, List<Addressee> readers) {
        readers.forEach(reader -> tell(reader, new SavingsGoalCreatedNotification(reader.username(), gesture.actorName(),
            gesture.spaceName(), goal, finance(gesture.spaceId()))));
    }

    @Override
    public void savingsGoalUpdated(Gesture gesture, GoalFacts before, GoalFacts after, List<Addressee> readers) {
        readers.forEach(reader -> tell(reader, new SavingsGoalUpdatedNotification(reader.username(), gesture.actorName(),
            gesture.spaceName(), before, after, finance(gesture.spaceId()))));
    }

    @Override
    public void savingsGoalDeleted(Gesture gesture, GoalFacts goal, BigDecimal saved, List<MemberShare> savedByMember,
                                   List<Addressee> readers) {
        readers.forEach(reader -> tell(reader, new SavingsGoalDeletedNotification(reader.username(), gesture.actorName(),
            gesture.spaceName(), goal, saved, savedByMember, finance(gesture.spaceId()))));
    }

    @Override
    public void settlementRecorded(Gesture gesture, SettlementAuthor author, BigDecimal amount, LocalDate date,
                                   Standing standing, BigDecimal remaining, Addressee reader) {
        tell(reader, new SettlementRecordedNotification(reader.username(), gesture.actorName(), gesture.spaceName(), author,
            amount, date, standing, remaining, finance(gesture.spaceId())));
    }

    @Override
    public void openingBalanceSet(Gesture gesture, BigDecimal amount, LocalDate asOf, List<Addressee> readers) {
        readers.forEach(reader -> tell(reader, new OpeningBalanceChangedNotification(reader.username(), gesture.actorName(),
            gesture.spaceName(), false, amount, asOf, finance(gesture.spaceId()))));
    }

    @Override
    public void openingBalanceCleared(Gesture gesture, List<Addressee> readers) {
        readers.forEach(reader -> tell(reader, new OpeningBalanceChangedNotification(reader.username(), gesture.actorName(),
            gesture.spaceName(), true, null, null, finance(gesture.spaceId()))));
    }

    @Override
    public void budgetCrossed(Gesture gesture, BudgetStatus reached, BudgetAlert alert, List<Addressee> readers) {
        readers.forEach(reader -> tell(reader, reached == BudgetStatus.OVER
            ? new BudgetExceededNotification(reader.username(), gesture.actorName(), gesture.spaceName(), alert,
                finance(gesture.spaceId()))
            : new BudgetWarningNotification(reader.username(), gesture.actorName(), gesture.spaceName(), alert,
                finance(gesture.spaceId()))));
    }

    /** Whose deposit it is to this reader: the author's own, the reader's, or another member's. */
    private static ContributorSide sideOf(SavingsDeposit deposit, Gesture gesture, Addressee reader) {
        if (deposit.contributorId().equals(gesture.actorId())) {
            return ContributorSide.AUTHOR;
        }
        return deposit.contributorId().equals(reader.userId()) ? ContributorSide.READER : ContributorSide.OTHER_MEMBER;
    }

    private void tell(Addressee reader, Notification notification) {
        notify.notify(new NotificationRequest(reader.userId(), notification, null));
    }

    private static AppPath finance(UUID spaceId) {
        return new AppPath("/s/" + spaceId + "/finance");
    }
}
