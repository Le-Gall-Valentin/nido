package com.nido.api.mail;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.mail.application.handler.DisabledSendMailHandler;
import com.nido.api.mail.application.port.in.CancelPendingMailsUseCase;
import com.nido.api.mail.application.port.in.DispatchPendingMailsUseCase;
import com.nido.api.mail.application.port.in.MailAvailabilityQuery;
import com.nido.api.mail.application.port.in.SendMailUseCase;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailRequest;
import com.nido.api.mail.domain.model.Recipient;
import com.nido.api.mail.domain.port.out.MailDispatchTriggerPort;
import com.nido.api.mail.domain.port.out.MailOutboxPort;
import com.nido.api.mail.domain.port.out.MailRendererPort;
import com.nido.api.mail.domain.port.out.MailTransportPort;
import com.nido.api.mail.infrastructure.config.MailSettings;
import com.nido.api.mail.infrastructure.scheduler.MailDispatchScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/** The default for every installation without NIDO_SMTP_HOST: nothing that could send a mail exists. */
@IntegrationTestConfig
class MailDisabledIT {

    @Autowired ApplicationContext context;
    @Autowired SendMailUseCase sendMail;
    @Autowired CancelPendingMailsUseCase cancelPendingMails;
    @Autowired MailAvailabilityQuery availability;
    @Autowired JdbcClient jdbc;

    @Test
    void nothing_that_could_send_a_mail_exists() {
        for (Class<?> type : List.of(MailTransportPort.class, MailRendererPort.class, MailOutboxPort.class,
                MailDispatchTriggerPort.class, DispatchPendingMailsUseCase.class, JavaMailSender.class,
                MailDispatchScheduler.class, MailSettings.class)) {
            assertThat(context.getBeanNamesForType(type)).as(type.getSimpleName()).isEmpty();
        }
    }

    @Test
    void a_mail_is_accepted_and_goes_nowhere() {
        jdbc.sql("DELETE FROM mail_outbox").update();

        sendMail.send(MailRequest.of(new Recipient("jane@test.local", "Jane"), Locale.FRENCH,
            new KitSampleMail("jane", new AppPath("/x"))));

        assertThat(sendMail).isInstanceOf(DisabledSendMailHandler.class);
        assertThat(jdbc.sql("SELECT count(*) FROM mail_outbox").query(Integer.class).single()).isZero();
    }

    @Test
    void mail_says_it_is_unavailable() {
        assertThat(availability.isAvailable()).isFalse();
    }

    @Test
    void there_is_nothing_queued_to_withdraw() {
        assertThat(cancelPendingMails.cancelPendingMailsTo("jane@test.local")).isZero();
    }
}
