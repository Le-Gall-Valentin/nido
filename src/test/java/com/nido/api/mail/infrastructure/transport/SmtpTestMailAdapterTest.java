package com.nido.api.mail.infrastructure.transport;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.nido.api.mail.domain.model.MailSettingsInput;
import com.nido.api.mail.domain.model.Recipient;
import com.nido.api.mail.domain.model.TestMailOutcome;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import jakarta.mail.AuthenticationFailedException;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;

import java.net.ServerSocket;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class SmtpTestMailAdapterTest {

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

    private static MailSettingsInput to(int port) {
        return new MailSettingsInput("127.0.0.1", port, "none", null, null, "Nido <nido@test.local>", "http://nido.test");
    }

    @Test
    void the_test_mail_arrives_in_the_language_asked() throws Exception {
        TestMailOutcome outcome = new SmtpTestMailAdapter()
            .send(to(smtp.getSmtp().getPort()), new Recipient("jane@test.local", null), Locale.FRENCH);

        assertThat(outcome).isEqualTo(new TestMailOutcome.Sent());
        assertThat(smtp.waitForIncomingEmail(5_000, 1)).isTrue();
        assertThat(smtp.getReceivedMessages()[0].getSubject()).isEqualTo("Nido peut envoyer des mails");
    }

    @Test
    void a_server_that_cannot_be_reached_says_why() throws Exception {
        int closed;
        try (ServerSocket socket = new ServerSocket(0)) {
            closed = socket.getLocalPort();
        }

        TestMailOutcome outcome = new SmtpTestMailAdapter().send(to(closed), new Recipient("jane@test.local", null), Locale.ENGLISH);

        assertThat(outcome).isEqualTo(new TestMailOutcome.Failed(TestMailOutcome.CONNECTION_REFUSED, null));
    }

    @Test
    void a_server_that_does_not_speak_smtp_is_named_as_such_and_its_banner_is_never_repeated() throws Exception {
        try (ServerSocket other = new ServerSocket(0)) {
            Thread answering = new Thread(() -> {
                try (var socket = other.accept()) {
                    socket.getOutputStream().write("-ERR unknown command, internal-redis-7.2\r\n".getBytes());
                    socket.getOutputStream().flush();
                } catch (Exception ignored) {
                    // the test only cares about what the adapter reports
                }
            });
            answering.start();

            TestMailOutcome outcome = new SmtpTestMailAdapter()
                .send(to(other.getLocalPort()), new Recipient("jane@test.local", null), Locale.ENGLISH);

            assertThat(outcome).isEqualTo(new TestMailOutcome.Failed(TestMailOutcome.NOT_AN_SMTP_SERVER, null));
            answering.join(5_000);
        }
    }

    @Test
    void what_an_smtp_server_says_is_kept_and_nothing_else() {
        assertThat(SmtpTestMailAdapter.classify(new MailAuthenticationException(
                new AuthenticationFailedException("535 5.7.8 Username and Password not accepted"))))
            .isEqualTo(new TestMailOutcome.Failed(TestMailOutcome.AUTHENTICATION_FAILED, "535 5.7.8 Username and Password not accepted"));
        assertThat(SmtpTestMailAdapter.classify(new MailSendException("x", new java.net.UnknownHostException("smtp.nowhere"))))
            .isEqualTo(new TestMailOutcome.Failed(TestMailOutcome.UNKNOWN_HOST, null));
        assertThat(SmtpTestMailAdapter.classify(new IllegalStateException("anything at all")))
            .isEqualTo(new TestMailOutcome.Failed(TestMailOutcome.FAILED, null));
    }
}
