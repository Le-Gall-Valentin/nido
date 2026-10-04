package fixtures.notifications.valid;

import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;

/** A second accepted notification, in another context than the first, so the order of the catalogue shows. */
@NotificationKind("agenda.reminder")
public record ReminderNotification(String title) implements Notification, MailContent {
    @Override
    public String template() {
        return "fixture/reminder";
    }
}
