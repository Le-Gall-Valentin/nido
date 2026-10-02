package fixtures.notifications.duplicate;

import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;

@NotificationKind("fixture.twin")
public record FirstTwinNotification(String text) implements Notification, MailContent {
    @Override
    public String template() {
        return "fixture/first-twin";
    }
}
