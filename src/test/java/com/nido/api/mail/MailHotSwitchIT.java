package com.nido.api.mail;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.SharedGreenMail;
import com.nido.api.instance.InstanceSettingsTestSupport;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.port.out.SettingsStorePort;
import com.nido.api.mail.application.port.in.MailAvailabilityQuery;
import com.nido.api.mail.application.port.in.SendMailUseCase;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailRequest;
import com.nido.api.mail.domain.model.Recipient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** Mail switched on and off from the saved settings, in a running application. */
@IntegrationTestConfig
@ExtendWith(SharedGreenMail.Starter.class)
class MailHotSwitchIT {

    @Autowired SettingsStorePort store;
    @Autowired MailAvailabilityQuery availability;
    @Autowired SendMailUseCase sendMail;
    @Autowired TransactionTemplate transactions;
    @Autowired JdbcClient jdbc;

    @AfterEach
    void clean() {
        InstanceSettingsTestSupport.clear(store);
        jdbc.sql("DELETE FROM mail_outbox").update();
    }

    @Test
    void mail_switched_on_delivers_without_a_restart_and_switched_off_stops() throws Exception {
        SharedGreenMail.server().purgeEmailFromAllMailboxes();
        assertThat(availability.isAvailable()).isFalse();

        store.save(Map.of(
            SettingKey.MAIL_HOST, Optional.of("127.0.0.1"),
            SettingKey.MAIL_PORT, Optional.of(String.valueOf(SharedGreenMail.PORT)),
            SettingKey.MAIL_SECURITY, Optional.of("none"),
            SettingKey.MAIL_FROM, Optional.of("Nido <nido@test.local>"),
            SettingKey.PUBLIC_URL, Optional.of("http://nido.test")), null, Instant.now());

        assertThat(availability.isAvailable()).isTrue();
        transactions.executeWithoutResult(status -> sendMail.send(MailRequest.of(
            new Recipient("jane@test.local", "Jane"), Locale.FRENCH, new KitSampleMail("jane", new AppPath("/x")))));
        assertThat(SharedGreenMail.server().waitForIncomingEmail(5_000, 1)).isTrue();
        assertThat(SharedGreenMail.server().getReceivedMessages()[0].getSubject()).isEqualTo("Échantillon du kit");

        InstanceSettingsTestSupport.clear(store);
        assertThat(availability.isAvailable()).isFalse();
    }
}
