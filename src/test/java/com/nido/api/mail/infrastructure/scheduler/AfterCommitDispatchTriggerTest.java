package com.nido.api.mail.infrastructure.scheduler;

import com.nido.api.mail.application.port.in.DispatchPendingMailsUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AfterCommitDispatchTriggerTest {

    private final AtomicInteger runs = new AtomicInteger();
    private final DispatchPendingMailsUseCase dispatch = () -> { runs.incrementAndGet(); return 0; };
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AfterCommitDispatchTrigger trigger = new AfterCommitDispatchTrigger(dispatch, executor);

    @AfterEach
    void cleanUp() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        executor.shutdownNow();
    }

    private int runsOnceSettled() throws InterruptedException {
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        return runs.get();
    }

    @Test
    void waits_for_the_commit() throws Exception {
        TransactionSynchronizationManager.initSynchronization();

        trigger.wakeUpAfterCommit();
        assertThat(runs.get()).isZero();

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        assertThat(runsOnceSettled()).isEqualTo(1);
    }

    @Test
    void a_rollback_dispatches_nothing() throws Exception {
        TransactionSynchronizationManager.initSynchronization();

        trigger.wakeUpAfterCommit();
        TransactionSynchronizationManager.getSynchronizations()
            .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(runsOnceSettled()).isZero();
    }

    @Test
    void outside_a_transaction_it_dispatches_right_away() throws Exception {
        trigger.wakeUpAfterCommit();

        assertThat(runsOnceSettled()).isEqualTo(1);
    }

    @Test
    void a_failing_dispatch_does_not_stop_the_next_one() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AfterCommitDispatchTrigger failingOnce = new AfterCommitDispatchTrigger(() -> {
            if (calls.incrementAndGet() == 1) {
                throw new IllegalStateException("database away");
            }
            return 0;
        }, executor);

        failingOnce.wakeUp();
        Thread.sleep(200);
        failingOnce.wakeUp();
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        assertThat(calls.get()).isEqualTo(2);
    }
}
