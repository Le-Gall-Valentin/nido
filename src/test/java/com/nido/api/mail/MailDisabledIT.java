package com.nido.api.mail;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.mail.application.port.in.CancelPendingMailsUseCase;
import com.nido.api.mail.application.port.in.DispatchPendingMailsUseCase;
import com.nido.api.mail.application.port.in.MailAvailabilityQuery;
import com.nido.api.mail.application.port.in.SendMailUseCase;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailRequest;
import com.nido.api.mail.domain.model.Recipient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/** Mail off — no NIDO_SMTP_HOST and nothing saved from the settings page: a mail is accepted and goes nowhere. */
@IntegrationTestConfig
class MailDisabledIT {

    @Autowired SendMailUseCase sendMail;
    @Autowired CancelPendingMailsUseCase cancelPendingMails;
    @Autowired DispatchPendingMailsUseCase dispatch;
    @Autowired MailAvailabilityQuery availability;
    @Autowired JdbcClient jdbc;

    @Test
    void a_mail_is_accepted_and_goes_nowhere() {
        jdbc.sql("DELETE FROM mail_outbox").update();

        sendMail.send(MailRequest.of(new Recipient("jane@test.local", "Jane"), Locale.FRENCH,
            new KitSampleMail("jane", new AppPath("/x"))));

        assertThat(jdbc.sql("SELECT count(*) FROM mail_outbox").query(Integer.class).single()).isZero();
    }

    @Test
    void mail_says_it_is_unavailable() {
        assertThat(availability.isAvailable()).isFalse();
    }

    @Test
    void a_dispatch_sends_nothing() {
        assertThat(dispatch.dispatch()).isZero();
    }

    @Test
    void there_is_nothing_queued_to_withdraw() {
        jdbc.sql("DELETE FROM mail_outbox").update();

        assertThat(cancelPendingMails.cancelPendingMailsTo("jane@test.local")).isZero();
    }
}
