package fixtures.notifications.valid;

import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;

/**
 * A notification the catalogue accepts. Outside com.nido.api on purpose: the application scans that
 * package at startup, test classes included, and would pick a test notification up.
 */
@NotificationKind("fixture.greeting")
public record GreetingNotification(String username) implements Notification, MailContent {
    @Override
    public String template() {
        return "fixture/greeting";
    }
}
