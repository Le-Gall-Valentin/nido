package com.nido.api.mail.infrastructure.transport;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.nido.api.mail.domain.model.MailSettingsInput;
import com.nido.api.mail.domain.model.Recipient;
import com.nido.api.mail.domain.model.TestMailOutcome;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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

        assertThat(outcome).isInstanceOfSatisfying(TestMailOutcome.Failed.class,
            failed -> assertThat(failed.detail()).isNotBlank().hasSizeLessThanOrEqualTo(300));
    }
}
