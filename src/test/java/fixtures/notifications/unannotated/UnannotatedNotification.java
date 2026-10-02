package fixtures.notifications.unannotated;

import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;

/** Forgets to declare its kind. */
public record UnannotatedNotification(String text) implements Notification, MailContent {
    @Override
    public String template() {
        return "fixture/unannotated";
    }
}
