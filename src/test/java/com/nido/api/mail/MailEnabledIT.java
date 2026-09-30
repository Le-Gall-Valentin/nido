package com.nido.api.mail;

import com.nido.api.MailIntegrationTestConfig;
import com.nido.api.SharedGreenMail;
import com.nido.api.mail.application.port.in.MailAvailabilityQuery;
import com.nido.api.mail.application.port.in.SendMailUseCase;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailRequest;
import com.nido.api.mail.domain.model.Recipient;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.HealthContributors;
import org.springframework.boot.health.contributor.ReactiveHealthContributors;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.boot.health.registry.ReactiveHealthContributorRegistry;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/** Mail on, end to end: a real transaction, the real outbox, the real dispatcher, an SMTP server. */
@MailIntegrationTestConfig
class MailEnabledIT {

    @Autowired SendMailUseCase sendMail;
    @Autowired MailAvailabilityQuery availability;
    @Autowired TransactionTemplate transactions;
    @Autowired JdbcClient jdbc;
    @Autowired HealthContributorRegistry blockingContributors;
    @Autowired ReactiveHealthContributorRegistry reactiveContributors;

    @BeforeEach
    void clean() throws Exception {
        jdbc.sql("DELETE FROM mail_outbox").update();
        SharedGreenMail.server().purgeEmailFromAllMailboxes();
    }

    private static MailRequest request() {
        return MailRequest.of(new Recipient("jane@test.local", "Jane Doe"), Locale.FRENCH,
            new KitSampleMail("jane", new AppPath("/somewhere")));
    }

    private int outboxRows() {
        return jdbc.sql("SELECT count(*) FROM mail_outbox").query(Integer.class).single();
    }

    private void awaitEmptyOutbox() throws InterruptedException {
        for (int i = 0; i < 50 && outboxRows() > 0; i++) {
            Thread.sleep(100);
        }
    }

    @Test
    void a_mail_sent_in_a_committed_transaction_is_delivered_and_leaves_the_outbox() throws Exception {
        transactions.executeWithoutResult(status -> sendMail.send(request()));

        assertThat(SharedGreenMail.server().waitForIncomingEmail(10_000, 1)).isTrue();
        MimeMessage received = SharedGreenMail.server().getReceivedMessages()[0];
        assertThat(received.getSubject()).isEqualTo("Échantillon du kit");
        awaitEmptyOutbox();
        assertThat(outboxRows()).isZero();
    }

    @Test
    void a_mail_sent_in_a_rolled_back_transaction_is_never_delivered() {
        transactions.executeWithoutResult(status -> {
            sendMail.send(request());
            status.setRollbackOnly();
        });

        assertThat(SharedGreenMail.server().waitForIncomingEmail(1_500, 1)).isFalse();
        assertThat(outboxRows()).isZero();
    }

    @Test
    void mail_says_it_is_available() {
        assertThat(availability.isAvailable()).isTrue();
    }

    @Test
    void the_smtp_server_is_not_part_of_the_health_verdict() {
        // Its outage would otherwise restart the container in a loop (application.yaml, management.health.mail).
        assertThat(blockingContributors.stream().map(HealthContributors.Entry::name))
            .noneSatisfy(name -> assertThat(name).containsIgnoringCase("mail"));
        assertThat(reactiveContributors.stream().map(ReactiveHealthContributors.Entry::name))
            .noneSatisfy(name -> assertThat(name).containsIgnoringCase("mail"));
    }
}
