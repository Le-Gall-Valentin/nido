package com.nido.api.notifications.infrastructure.delivery;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.nido.api.notifications.domain.model.NotificationRequest;
import com.nido.api.notifications.domain.model.NotificationType;
import fixtures.notifications.valid.GreetingNotification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AfterCommitDeliveryAdapterTest {

    private static final NotificationType GREETING = new NotificationType("fixture.greeting");

    private final List<NotificationRequest> delivered = new ArrayList<>();
    private final AfterCommitDeliveryAdapter adapter = new AfterCommitDeliveryAdapter((type, request) -> delivered.add(request));
    private final NotificationRequest first = request();
    private final NotificationRequest second = request();
    private final ListAppender<ILoggingEvent> logged = new ListAppender<>();

    private static NotificationRequest request() {
        return new NotificationRequest(UUID.randomUUID(), new GreetingNotification("jane"), null);
    }

    @BeforeEach
    void listen() {
        logged.start();
        ((Logger) LoggerFactory.getLogger(AfterCommitDeliveryAdapter.class)).addAppender(logged);
    }

    @AfterEach
    void cleanUp() {
        ((Logger) LoggerFactory.getLogger(AfterCommitDeliveryAdapter.class)).detachAppender(logged);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static void commit() {
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
    }

    @Test
    void waits_for_the_commit() {
        TransactionSynchronizationManager.initSynchronization();

        adapter.deliverAfterCommit(GREETING, first);
        assertThat(delivered).isEmpty();

        commit();
        assertThat(delivered).containsExactly(first);
    }

    @Test
    void a_rollback_delivers_nothing() {
        TransactionSynchronizationManager.initSynchronization();

        adapter.deliverAfterCommit(GREETING, first);
        TransactionSynchronizationManager.getSynchronizations()
            .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(delivered).isEmpty();
    }

    @Test
    void outside_a_transaction_it_delivers_right_away() {
        adapter.deliverAfterCommit(GREETING, first);

        assertThat(delivered).containsExactly(first);
    }

    @Test
    void a_delivery_that_fails_is_logged_and_goes_no_further() {
        AfterCommitDeliveryAdapter failing = new AfterCommitDeliveryAdapter((type, request) -> {
            throw new IllegalStateException("template broken");
        });

        assertThatCode(() -> failing.deliverAfterCommit(GREETING, first)).doesNotThrowAnyException();

        assertThat(logged.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getFormattedMessage()).isEqualTo("Notification fixture.greeting to account "
                + first.recipientId() + " not delivered; the change it reports stands");
        });
    }

    @Test
    void one_failing_delivery_does_not_stop_the_next() {
        AfterCommitDeliveryAdapter failingFirst = new AfterCommitDeliveryAdapter((type, request) -> {
            if (request == first) {
                throw new IllegalStateException("template broken");
            }
            delivered.add(request);
        });
        TransactionSynchronizationManager.initSynchronization();

        failingFirst.deliverAfterCommit(GREETING, first);
        failingFirst.deliverAfterCommit(GREETING, second);
        commit();

        assertThat(delivered).containsExactly(second);
    }
}
