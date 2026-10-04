package com.nido.api.notifications.infrastructure.delivery;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.nido.api.notifications.application.port.in.DeliverNotificationUseCase;
import com.nido.api.notifications.domain.model.NotificationRequest;
import com.nido.api.notifications.domain.model.NotificationType;
import com.nido.api.shared.model.Language;
import fixtures.notifications.valid.GreetingNotification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AfterCommitDeliveryAdapterTest {

    private static final NotificationType GREETING = new NotificationType("fixture.greeting");

    /** What reached the use case: the request, and the language of whoever acted. */
    private record Delivered(NotificationRequest request, Language actorLanguage) {}

    private final List<Delivered> delivered = new ArrayList<>();
    private final DeliverNotificationUseCase recording =
        (type, request, actorLanguage) -> delivered.add(new Delivered(request, actorLanguage));
    /** The worker, run by hand: whatever it holds has not been delivered yet. */
    private final List<Runnable> queued = new ArrayList<>();
    private final Executor worker = queued::add;
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
        RequestContextHolder.resetRequestAttributes();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static void commit() {
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
    }

    private void runTheWorker() {
        List<Runnable> tasks = new ArrayList<>(queued);
        queued.clear();
        tasks.forEach(Runnable::run);
    }

    @Test
    void waits_for_the_commit_then_hands_over_to_the_worker() {
        AfterCommitDeliveryAdapter adapter = new AfterCommitDeliveryAdapter(recording, worker);
        TransactionSynchronizationManager.initSynchronization();

        adapter.deliverAfterCommit(GREETING, first);
        assertThat(queued).isEmpty();

        commit();
        assertThat(queued).hasSize(1);
        assertThat(delivered).as("the committing thread does not deliver").isEmpty();

        runTheWorker();
        assertThat(delivered).extracting(Delivered::request).containsExactly(first);
    }

    @Test
    void a_rollback_delivers_nothing() {
        AfterCommitDeliveryAdapter adapter = new AfterCommitDeliveryAdapter(recording, worker);
        TransactionSynchronizationManager.initSynchronization();

        adapter.deliverAfterCommit(GREETING, first);
        TransactionSynchronizationManager.getSynchronizations()
            .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(queued).isEmpty();
    }

    @Test
    void outside_a_transaction_it_is_handed_over_at_once() {
        new AfterCommitDeliveryAdapter(recording, worker).deliverAfterCommit(GREETING, first);
        runTheWorker();

        assertThat(delivered).extracting(Delivered::request).containsExactly(first);
    }

    @Test
    void it_takes_along_the_language_of_the_request_that_asked() {
        MockHttpServletRequest asking = new MockHttpServletRequest();
        asking.addHeader("Accept-Language", "fr-FR,fr;q=0.9");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(asking));
        AfterCommitDeliveryAdapter adapter = new AfterCommitDeliveryAdapter(recording, worker);
        TransactionSynchronizationManager.initSynchronization();

        adapter.deliverAfterCommit(GREETING, first);
        commit();
        // The worker has no request of its own: by then the one that asked may be gone.
        RequestContextHolder.resetRequestAttributes();
        runTheWorker();

        assertThat(delivered).containsExactly(new Delivered(first, Language.FR));
    }

    @Test
    void without_a_request_the_language_of_whoever_acts_is_unknown() {
        new AfterCommitDeliveryAdapter(recording, worker).deliverAfterCommit(GREETING, first);
        runTheWorker();

        assertThat(delivered).containsExactly(new Delivered(first, null));
    }

    @Test
    void a_delivery_that_fails_is_logged_by_its_class_only_and_goes_no_further() {
        AfterCommitDeliveryAdapter failing = new AfterCommitDeliveryAdapter((type, request, actorLanguage) -> {
            throw new IllegalStateException("could quote what it was writing: jane@test.local");
        }, Runnable::run);

        assertThatCode(() -> failing.deliverAfterCommit(GREETING, first)).doesNotThrowAnyException();

        assertThat(logged.list).filteredOn(event -> event.getLevel() == Level.ERROR).singleElement().satisfies(event -> {
            assertThat(event.getFormattedMessage()).isEqualTo("Notification fixture.greeting to account "
                + first.recipientId() + " not delivered (IllegalStateException); the change it reports stands");
            assertThat(event.getThrowableProxy()).isNull();
        });
    }

    @Test
    void one_failing_delivery_does_not_stop_the_next() {
        AfterCommitDeliveryAdapter failingFirst = new AfterCommitDeliveryAdapter((type, request, actorLanguage) -> {
            if (request == first) {
                throw new IllegalStateException("template broken");
            }
            delivered.add(new Delivered(request, actorLanguage));
        }, worker);
        TransactionSynchronizationManager.initSynchronization();

        failingFirst.deliverAfterCommit(GREETING, first);
        failingFirst.deliverAfterCommit(GREETING, second);
        commit();
        runTheWorker();

        assertThat(delivered).extracting(Delivered::request).containsExactly(second);
    }
}
