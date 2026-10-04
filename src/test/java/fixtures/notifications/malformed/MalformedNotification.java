package fixtures.notifications.malformed;

import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;

@NotificationKind("Fixture.Bad Code")
public record MalformedNotification(String text) implements Notification, MailContent {
    @Override
    public String template() {
        return "fixture/malformed";
    }
}
