package com.nido.api.calendar.infrastructure.persistence.adapter;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Postgres advisory locks of the calendar, released when their transaction ends. What a limit or a duplicate check reads
 * and then writes is put in line with another request doing the same — a row lock cannot hold rows not written yet.
 * A wait is bounded, as the account lock's (PostgresAccountLock).
 */
@Component
public class CalendarAdvisoryLocks {

    /** The calendar's own space of advisory keys; the second key is what is locked. */
    static final int NAMESPACE = 1_003;
    static final String LOCK_TIMEOUT = "10s";

    private final JdbcClient jdbc;

    public CalendarAdvisoryLocks(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Waits for whoever holds {@code what}, then holds it until the transaction ends. */
    void lock(String what) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("A calendar lock lasts until its transaction ends: take it inside one");
        }
        jdbc.sql("SET LOCAL lock_timeout = '" + LOCK_TIMEOUT + "'").update();
        jdbc.sql("SELECT pg_advisory_xact_lock(:namespace, hashtext(:what))")
            .param("namespace", NAMESPACE)
            .param("what", what)
            .query((rs, rowNum) -> Boolean.TRUE)
            .single();
    }
}
