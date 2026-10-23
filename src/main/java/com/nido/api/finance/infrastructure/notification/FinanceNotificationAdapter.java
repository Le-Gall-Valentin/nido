package com.nido.api.finance.infrastructure.notification;

import com.nido.api.finance.domain.model.Gesture;
import com.nido.api.finance.domain.model.Involved;
import com.nido.api.finance.domain.model.OperationFacts;
import com.nido.api.finance.domain.model.SeriesFacts;
import com.nido.api.finance.domain.port.out.FinanceNotificationPort;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.notifications.application.port.in.NotifyUseCase;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationRequest;
import com.nido.api.space.domain.model.Addressee;
import org.springframework.stereotype.Component;

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

    private void tell(Addressee reader, Notification notification) {
        notify.notify(new NotificationRequest(reader.userId(), notification, null));
    }

    private static AppPath finance(UUID spaceId) {
        return new AppPath("/s/" + spaceId + "/finance");
    }
}
