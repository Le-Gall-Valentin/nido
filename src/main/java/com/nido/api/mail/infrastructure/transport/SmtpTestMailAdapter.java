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
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.SendFailedException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * One mail, now, with settings that may not be saved yet — the button next to the form.
 *
 * <p>The form chooses the host and the port, so this is a way to make the server open a connection
 * anywhere. What comes back is therefore a reason, not the raw error: the server's own words are kept
 * only when it answered as an SMTP server does — a line starting with a reply code, "535 …" — so that
 * pointing the form at another kind of service never repeats its banner. Never logged: an SMTP reply
 * can quote addresses.
 */
@Component
public class SmtpTestMailAdapter implements TestMailPort {

    private static final int REPLY_LENGTH = 300;
    private static final Pattern SMTP_REPLY = Pattern.compile("^[2-5]\\d\\d[ -].*");

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
            return classify(e);
        }
    }

    static TestMailOutcome.Failed classify(Throwable failure) {
        List<Throwable> chain = chain(failure);
        if (any(chain, AuthenticationFailedException.class)) {
            return new TestMailOutcome.Failed(TestMailOutcome.AUTHENTICATION_FAILED, smtpReply(chain));
        }
        if (any(chain, UnknownHostException.class)) {
            return new TestMailOutcome.Failed(TestMailOutcome.UNKNOWN_HOST, null);
        }
        if (any(chain, ConnectException.class)) {
            return new TestMailOutcome.Failed(TestMailOutcome.CONNECTION_REFUSED, null);
        }
        if (any(chain, SocketTimeoutException.class)) {
            return new TestMailOutcome.Failed(TestMailOutcome.TIMEOUT, null);
        }
        if (any(chain, SSLException.class) || mentions(chain, "starttls")) {
            return new TestMailOutcome.Failed(TestMailOutcome.TLS_FAILED, null);
        }
        if (mentions(chain, "bad greeting")) {
            return new TestMailOutcome.Failed(TestMailOutcome.NOT_AN_SMTP_SERVER, null);
        }
        if (any(chain, SendFailedException.class)) {
            return new TestMailOutcome.Failed(TestMailOutcome.REJECTED, smtpReply(chain));
        }
        return new TestMailOutcome.Failed(TestMailOutcome.FAILED, null);
    }

    /** The failure, its causes, and the exceptions a Spring send failure carries for each message. */
    private static List<Throwable> chain(Throwable failure) {
        List<Throwable> chain = new ArrayList<>();
        List<Throwable> pending = new ArrayList<>(List.of(failure));
        while (!pending.isEmpty()) {
            Throwable next = pending.removeFirst();
            for (Throwable t = next; t != null && !chain.contains(t); t = t.getCause()) {
                chain.add(t);
                if (t instanceof MailSendException send) {
                    pending.addAll(Arrays.asList(send.getMessageExceptions()));
                }
            }
        }
        return chain;
    }

    private static boolean any(List<Throwable> chain, Class<? extends Throwable> type) {
        return chain.stream().anyMatch(type::isInstance);
    }

    private static boolean mentions(List<Throwable> chain, String words) {
        return chain.stream().anyMatch(t -> t.getMessage() != null && t.getMessage().toLowerCase(Locale.ROOT).contains(words));
    }

    private static String smtpReply(List<Throwable> chain) {
        return chain.stream()
            .map(Throwable::getMessage)
            .filter(message -> message != null)
            .map(message -> message.strip().lines().findFirst().orElse(""))
            .filter(line -> SMTP_REPLY.matcher(line).matches())
            .findFirst()
            .map(line -> line.length() > REPLY_LENGTH ? line.substring(0, REPLY_LENGTH) : line)
            .orElse(null);
    }
}
