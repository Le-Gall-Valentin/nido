package com.nido.api.mail.infrastructure.scheduler;

import com.nido.api.infrastructure.config.ConditionalOnMailEnabled;
import com.nido.api.mail.application.port.in.DispatchPendingMailsUseCase;
import com.nido.api.mail.domain.port.out.MailDispatchTriggerPort;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs a dispatch on one dedicated thread, right after the transaction that queued a mail commits.
 *
 * <p>One thread: dispatches in this JVM never race each other, and the request that sent the mail
 * never waits for SMTP. Wake-ups that arrive while a run is already queued add nothing — that run will
 * see their mails.
 */
@Component
@ConditionalOnMailEnabled
public class AfterCommitDispatchTrigger implements MailDispatchTriggerPort {

    private static final Logger log = LoggerFactory.getLogger(AfterCommitDispatchTrigger.class);

    private final DispatchPendingMailsUseCase dispatch;
    private final ExecutorService executor;
    private final AtomicBoolean runQueued = new AtomicBoolean(false);

    @Autowired
    public AfterCommitDispatchTrigger(DispatchPendingMailsUseCase dispatch) {
        this(dispatch, Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "mail-dispatch");
            thread.setDaemon(true);
            return thread;
        }));
    }

    AfterCommitDispatchTrigger(DispatchPendingMailsUseCase dispatch, ExecutorService executor) {
        this.dispatch = dispatch;
        this.executor = executor;
    }

    @Override
    public void wakeUpAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            wakeUp();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                wakeUp();
            }
        });
    }

    /** Asks for one dispatch run soon. */
    public void wakeUp() {
        if (!runQueued.compareAndSet(false, true)) {
            return;
        }
        try {
            executor.execute(() -> {
                runQueued.set(false);
                try {
                    dispatch.dispatch();
                } catch (RuntimeException e) {
                    log.error("Mail dispatch failed; the next sweep tries again", e);
                }
            });
        } catch (RejectedExecutionException e) {
            // Shutting down. The mail stays in the outbox, and the next start's sweep sends it.
            runQueued.set(false);
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
