package com.nido.api.instance.application.handler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.nido.api.instance.domain.model.KeyFingerprint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptionKeyFingerprintHandlerTest {

    private static final String KEY = "the-key-of-this-installation-32chars";

    private final InstanceFakes.MemoryState state = new InstanceFakes.MemoryState();
    private final EncryptionKeyFingerprintHandler handler = new EncryptionKeyFingerprintHandler(state);
    private final Logger logger = (Logger) LoggerFactory.getLogger(EncryptionKeyFingerprintHandler.class);
    private final ListAppender<ILoggingEvent> logged = new ListAppender<>();

    @BeforeEach
    void listen() {
        logged.start();
        logger.addAppender(logged);
    }

    @AfterEach
    void stopListening() {
        logger.detachAppender(logged);
    }

    private KeyFingerprint recorded() {
        KeyFingerprint fingerprint = KeyFingerprint.of(KEY);
        state.recordFingerprint(fingerprint, false);
        return fingerprint;
    }

    @Test
    void a_confirmed_fingerprint_stays_for_good() {
        KeyFingerprint fingerprint = recorded();

        handler.confirm(fingerprint);
        handler.forget(fingerprint);

        assertThat(state.fingerprint).isEqualTo(fingerprint);
        assertThat(state.fingerprintConfirmed).isTrue();
    }

    @Test
    void an_unconfirmed_fingerprint_is_erased_and_the_log_says_so() {
        KeyFingerprint fingerprint = recorded();

        handler.forget(fingerprint);

        assertThat(state.fingerprint).isNull();
        assertThat(logged.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.WARN);
        assertThat(logged.list.getFirst().getFormattedMessage()).contains("was erased");
    }

    @Test
    void a_fingerprint_another_start_replaced_is_left_alone_and_the_log_does_not_claim_it_was_erased() {
        KeyFingerprint ours = recorded();
        KeyFingerprint theirs = recorded();

        handler.forget(ours);

        assertThat(state.fingerprint).isEqualTo(theirs);
        assertThat(logged.list).extracting(ILoggingEvent::getFormattedMessage).noneMatch(message -> message.contains("was erased"));
    }
}
