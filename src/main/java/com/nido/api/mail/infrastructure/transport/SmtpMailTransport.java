package com.nido.api.mail.infrastructure.transport;

import com.nido.api.mail.domain.model.DeliveryOutcome;
import com.nido.api.mail.domain.model.OutgoingMail;
import com.nido.api.mail.domain.port.out.MailTransportPort;
import com.nido.api.mail.infrastructure.config.MailConfigurationAdapter;
import com.nido.api.mail.infrastructure.config.MailSettings;
import com.nido.api.mail.infrastructure.render.MailBranding;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Sends one mail: HTML and text as alternatives, the logo attached inline.
 *
 * <p>Failures are sorted into those worth retrying and those that are not. A recipient the server
 * refuses, or an address that cannot even be written, will be refused the same way next time. A
 * refused login is retried: it is a configuration problem, and retrying keeps the mail alive while
 * someone fixes it — every failure is logged by the dispatcher either way.
 *
 * <p>A reason names exception classes only: their messages quote addresses.
 *
 * <p>The SMTP client follows the settings: rebuilt when they change, kept as long as they do not.
 */
@Component
public class SmtpMailTransport implements MailTransportPort {

    private record Client(MailSettings settings, JavaMailSender sender) {}

    private final Supplier<Optional<Client>> client;

    /** The server of the moment: the SMTP client is rebuilt when the settings change, kept otherwise. */
    @Autowired
    public SmtpMailTransport(MailConfigurationAdapter configuration) {
        AtomicReference<Client> last = new AtomicReference<>();
        this.client = () -> configuration.settings().map(settings -> {
            Client current = last.get();
            if (current != null && current.settings().equals(settings)) {
                return current;
            }
            Client fresh = new Client(settings, SmtpSenderFactory.create(settings));
            last.set(fresh);
            return fresh;
        });
    }

    /** One fixed server, whatever the settings say. */
    SmtpMailTransport(JavaMailSender sender, MailSettings settings) {
        Client fixed = new Client(settings, sender);
        this.client = () -> Optional.of(fixed);
    }

    @Override
    public DeliveryOutcome deliver(OutgoingMail mail) {
        Optional<Client> current = client.get();
        if (current.isEmpty()) {
            // Switched off between the claim and the send: the mail stays queued for when it comes back.
            return new DeliveryOutcome.TemporaryFailure("MailSwitchedOff");
        }
        JavaMailSender sender = current.get().sender();
        MimeMessage message;
        try {
            message = compose(sender, current.get().settings().from(), mail);
        } catch (MessagingException | UnsupportedEncodingException e) {
            return new DeliveryOutcome.PermanentFailure(describe(e));
        }
        try {
            sender.send(message);
            return new DeliveryOutcome.Sent();
        } catch (MailSendException e) {
            return refusesTheRecipient(e)
                ? new DeliveryOutcome.PermanentFailure(describe(e))
                : new DeliveryOutcome.TemporaryFailure(describe(e));
        } catch (MailException e) {
            return new DeliveryOutcome.TemporaryFailure(describe(e));
        }
    }

    static MimeMessage compose(JavaMailSender sender, InternetAddress from, OutgoingMail mail)
            throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
            StandardCharsets.UTF_8.name());
        helper.setFrom(from);
        InternetAddress to = new InternetAddress(mail.to().address(), true);
        if (mail.to().displayName() != null && !mail.to().displayName().isBlank()) {
            to.setPersonal(mail.to().displayName(), StandardCharsets.UTF_8.name());
        }
        helper.setTo(to);
        helper.setSubject(mail.content().subject());
        helper.setText(mail.content().text(), mail.content().html());
        // After setText: an inline part attached earlier would not land in the related multipart.
        helper.addInline(MailBranding.LOGO_CID, MailBranding.LOGO, "image/png");
        return message;
    }

    private static boolean refusesTheRecipient(MailSendException e) {
        return underlying(e)
            .anyMatch(t -> t instanceof SendFailedException refused
                && refused.getInvalidAddresses() != null
                && refused.getInvalidAddresses().length > 0);
    }

    /** What a send failure wraps: its own cause chain, then the exception of each message that failed. */
    private static Stream<Throwable> underlying(MailSendException e) {
        return Stream.concat(e.getFailedMessages().values().stream(), Arrays.stream(e.getMessageExceptions()))
            .flatMap(SmtpMailTransport::causes);
    }

    private static Stream<Throwable> causes(Throwable top) {
        List<Throwable> chain = new ArrayList<>();
        for (Throwable t = top; t != null && !chain.contains(t); t = t.getCause()) {
            chain.add(t);
        }
        return chain.stream();
    }

    private static String describe(Throwable failure) {
        Stream<Throwable> all = failure instanceof MailSendException send
            ? Stream.concat(causes(send), underlying(send))
            : causes(failure);
        return all.distinct().map(t -> t.getClass().getSimpleName()).collect(Collectors.joining(" > "));
    }
}
