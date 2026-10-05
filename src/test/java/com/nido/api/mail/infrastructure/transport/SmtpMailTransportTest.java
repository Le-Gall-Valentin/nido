package com.nido.api.mail.infrastructure.transport;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.nido.api.mail.domain.model.MailSettingsInput;
import com.nido.api.mail.domain.model.DeliveryOutcome;
import com.nido.api.mail.domain.model.OutgoingMail;
import com.nido.api.mail.domain.model.Recipient;
import com.nido.api.mail.domain.model.RenderedMail;
import com.nido.api.mail.infrastructure.config.MailSettings;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.net.ServerSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SmtpMailTransportTest {

    private static GreenMail smtp;

    @BeforeAll
    static void start() {
        smtp = new GreenMail(ServerSetupTest.SMTP.dynamicPort());
        smtp.start();
    }

    @AfterAll
    static void stop() {
        smtp.stop();
    }

    @BeforeEach
    void purge() throws Exception {
        smtp.purgeEmailFromAllMailboxes();
    }

    private static MailSettings settings(int port) {
        return MailSettings.check(new MailSettingsInput("127.0.0.1", port, "none", null, null,
            "Nido <nido@test.local>", "http://localhost:5173")).settings().orElseThrow();
    }

    private static SmtpMailTransport transportTo(int port) {
        MailSettings settings = settings(port);
        return new SmtpMailTransport(SmtpSenderFactory.create(settings, Duration.ofMillis(500)), settings);
    }

    private static OutgoingMail mail(String address, String name, String subject) {
        return new OutgoingMail(new Recipient(address, name),
            new RenderedMail(subject, "<html><body><p>Bonjour</p><img src=\"cid:nido-mark\"></body></html>", "Bonjour\n"));
    }

    private static List<Part> leaves(Part part) throws Exception {
        List<Part> leaves = new ArrayList<>();
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart child = multipart.getBodyPart(i);
                leaves.addAll(leaves(child));
            }
        } else {
            leaves.add(part);
        }
        return leaves;
    }

    @Test
    void delivers_both_bodies_and_the_inline_logo() throws Exception {
        DeliveryOutcome outcome = transportTo(smtp.getSmtp().getPort())
            .deliver(mail("jane@test.local", "Jane Doe", "Hello"));

        assertThat(outcome).isInstanceOf(DeliveryOutcome.Sent.class);
        MimeMessage received = smtp.getReceivedMessages()[0];
        assertThat(((InternetAddress) received.getFrom()[0]).getAddress()).isEqualTo("nido@test.local");
        assertThat(((InternetAddress) received.getFrom()[0]).getPersonal()).isEqualTo("Nido");
        List<Part> parts = leaves(received);
        assertThat(parts).anySatisfy(p -> {
            assertThat(p.isMimeType("text/plain")).isTrue();
            assertThat((String) p.getContent()).contains("Bonjour");
        });
        assertThat(parts).anySatisfy(p -> {
            assertThat(p.isMimeType("text/html")).isTrue();
            assertThat((String) p.getContent()).contains("cid:nido-mark");
        });
        assertThat(parts).anySatisfy(p -> {
            assertThat(p.isMimeType("image/png")).isTrue();
            assertThat(p.getHeader("Content-ID")).containsExactly("<nido-mark>");
        });
    }

    @Test
    void encodes_accented_subjects_and_display_names() throws Exception {
        transportTo(smtp.getSmtp().getPort())
            .deliver(mail("elodie@test.local", "Élodie Dupré", "Réinitialiser votre mot de passe Nido"));

        MimeMessage received = smtp.getReceivedMessages()[0];
        assertThat(received.getSubject()).isEqualTo("Réinitialiser votre mot de passe Nido");
        assertThat(((InternetAddress) received.getRecipients(MimeMessage.RecipientType.TO)[0]).getPersonal())
            .isEqualTo("Élodie Dupré");
    }

    @Test
    void mail_switched_off_between_the_claim_and_the_send_keeps_the_mail_queued() {
        SmtpMailTransport transport = new SmtpMailTransport(Optional::empty);

        assertThat(transport.deliver(mail("jane@test.local", null, "Hello")))
            .isEqualTo(new DeliveryOutcome.TemporaryFailure("MailSwitchedOff"));
    }

    @Test
    void the_server_of_the_moment_receives_the_mail() throws Exception {
        MailSettings current = settings(smtp.getSmtp().getPort());
        SmtpMailTransport transport = new SmtpMailTransport(() -> Optional.of(current));

        assertThat(transport.deliver(mail("jane@test.local", null, "Hello"))).isInstanceOf(DeliveryOutcome.Sent.class);
    }

    @Test
    void an_unreachable_server_is_a_temporary_failure() throws Exception {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }

        assertThat(transportTo(closedPort).deliver(mail("jane@test.local", null, "Hello")))
            .isInstanceOf(DeliveryOutcome.TemporaryFailure.class);
    }

    @Test
    void a_server_that_never_answers_is_a_temporary_failure_not_a_hang() throws Exception {
        // The socket accepts the connection through its backlog and then never says a word.
        try (ServerSocket silent = new ServerSocket(0)) {
            long started = System.nanoTime();

            DeliveryOutcome outcome = transportTo(silent.getLocalPort()).deliver(mail("jane@test.local", null, "Hello"));

            assertThat(outcome).isInstanceOf(DeliveryOutcome.TemporaryFailure.class);
            assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(5));
        }
    }

    @Test
    void a_malformed_recipient_is_a_permanent_failure() {
        assertThat(transportTo(smtp.getSmtp().getPort()).deliver(mail("not an address", null, "Hello")))
            .isInstanceOf(DeliveryOutcome.PermanentFailure.class);
    }

    @Test
    void a_recipient_the_server_refuses_is_a_permanent_failure_whose_reason_names_no_address() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
        MimeMessage any = new MimeMessage(Session.getInstance(new Properties()));
        SendFailedException refused = new SendFailedException("Invalid Addresses", null,
            new Address[0], new Address[0], new Address[]{new InternetAddress("gone@test.local")});
        doThrow(new MailSendException(Map.of(any, refused))).when(sender).send(any(MimeMessage.class));

        DeliveryOutcome outcome = new SmtpMailTransport(sender, settings(25)).deliver(mail("gone@test.local", null, "Hello"));

        assertThat(outcome).isInstanceOfSatisfying(DeliveryOutcome.PermanentFailure.class,
            failure -> assertThat(failure.reason()).contains("SendFailedException").doesNotContain("gone@test.local"));
    }

    @Test
    void a_refused_login_is_a_temporary_failure() {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailAuthenticationException("535 Authentication failed")).when(sender).send(any(MimeMessage.class));

        assertThat(new SmtpMailTransport(sender, settings(25)).deliver(mail("jane@test.local", null, "Hello")))
            .isInstanceOf(DeliveryOutcome.TemporaryFailure.class);
    }
}
