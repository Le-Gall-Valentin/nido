package fixtures.notifications.reserved;

import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;
import com.nido.api.shared.model.Role;

/** A kind only super-administrators may receive. */
@NotificationKind(value = "fixture.warden", roles = Role.SUPER_ADMIN)
public record WardenNotification(String username) implements Notification, MailContent {
    @Override
    public String template() {
        return "fixture/warden";
    }
}
