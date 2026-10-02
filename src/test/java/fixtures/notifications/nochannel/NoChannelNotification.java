package fixtures.notifications.nochannel;

import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;

/** Declares its kind but implements no channel's content type: nothing could ever deliver it. */
@NotificationKind("fixture.silent")
public record NoChannelNotification(String text) implements Notification {
}
