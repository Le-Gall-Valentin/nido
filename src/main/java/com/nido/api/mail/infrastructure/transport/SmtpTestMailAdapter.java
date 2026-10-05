package com.nido.api.mail.infrastructure.transport;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailSettingsInput;
import com.nido.api.mail.domain.model.OutgoingMail;
import com.nido.api.mail.domain.model.Recipient;
import com.nido.api.mail.domain.model.RenderedMail;
import com.nido.api.mail.domain.model.TestMailOutcome;
import com.nido.api.mail.domain.port.out.TestMailPort;
import com.nido.api.mail.infrastructure.config.MailSettings;
import com.nido.api.mail.infrastructure.render.ThymeleafMailRenderer;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * One mail, now, with settings that may not be saved yet — the button next to the form. Its failure is
 * shown to the administrator who pressed it, as the server worded it: the most useful thing to read
 * when a login is refused or a port is wrong. It is never logged — it can quote addresses.
 */
@Component
public class SmtpTestMailAdapter implements TestMailPort {

    private static final int DETAIL_LENGTH = 300;

    @Override
    public TestMailOutcome send(MailSettingsInput input, Recipient to, Locale locale) {
        MailSettings.Check check = MailSettings.check(input);
        if (check.settings().isEmpty()) {
            return new TestMailOutcome.Invalid(check.problems());
        }
        MailSettings settings = check.settings().get();
        RenderedMail rendered = new ThymeleafMailRenderer(settings.appUrl().toString()).render(new TestMail(new AppPath("/")), locale);
        JavaMailSenderImpl sender = SmtpSenderFactory.create(settings);
        try {
            sender.send(SmtpMailTransport.compose(sender, settings.from(), new OutgoingMail(to, rendered)));
            return new TestMailOutcome.Sent();
        } catch (Exception e) {
            return new TestMailOutcome.Failed(detail(e));
        }
    }

    static String detail(Throwable failure) {
        Throwable root = failure;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage() == null || root.getMessage().isBlank()
            ? root.getClass().getSimpleName() : root.getMessage().strip();
        return message.length() > DETAIL_LENGTH ? message.substring(0, DETAIL_LENGTH) : message;
    }
}
