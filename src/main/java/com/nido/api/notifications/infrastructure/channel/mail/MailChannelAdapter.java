package com.nido.api.notifications.infrastructure.channel.mail;

import com.nido.api.infrastructure.web.MailLanguage;
import com.nido.api.mail.application.port.in.MailAvailabilityQuery;
import com.nido.api.mail.application.port.in.SendMailUseCase;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.mail.domain.model.MailRequest;
import com.nido.api.mail.domain.model.Recipient;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationChannel;
import com.nido.api.notifications.domain.model.NotificationRecipient;
import com.nido.api.notifications.domain.model.NotificationType;
import com.nido.api.notifications.domain.port.out.NotificationChannelPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Notifications by mail, through the mail context. A notification goes by mail when its record is also a
 * {@link MailContent}: the record is handed to the mail context as it is, written now in the account's
 * language and queued in the caller's transaction. Present whether mail is on or off — off, it is simply
 * never available.
 */
@Component
public class MailChannelAdapter implements NotificationChannelPort {

    private static final Logger log = LoggerFactory.getLogger(MailChannelAdapter.class);

    private final SendMailUseCase sendMail;
    private final MailAvailabilityQuery availability;

    public MailChannelAdapter(SendMailUseCase sendMail, MailAvailabilityQuery availability) {
        this.sendMail = sendMail;
        this.availability = availability;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public boolean isAvailable() {
        return availability.isAvailable();
    }

    @Override
    public boolean supports(Class<? extends Notification> notificationClass) {
        return MailContent.class.isAssignableFrom(notificationClass);
    }

    @Override
    public void deliver(NotificationRecipient recipient, NotificationType type, Notification notification,
                        Instant expiresAt) {
        MailContent content = (MailContent) notification;
        if (recipient.email() == null || recipient.email().isBlank()) {
            log.warn("Account {} has no address: {} not sent", recipient.userId(), type.code());
            return;
        }
        sendMail.send(new MailRequest(new Recipient(recipient.email(), recipient.username()),
            MailLanguage.resolve(recipient.language()), content, expiresAt));
    }
}
