package com.nido.api.authentication.infrastructure.persistence.adapter;

import com.nido.api.authentication.domain.port.out.AccountLockPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * A Postgres advisory lock per account, released when the transaction ends.
 *
 * <p>Advisory rather than a row lock: the revocation of every session runs in a transaction of its
 * own while its caller — a password reset, a password change — has already updated the account's
 * credentials row, so a {@code SELECT … FOR UPDATE} on that row would wait on the caller for ever.
 * Nothing else takes this lock.
 *
 * <p>A wait is bounded: past {@link #LOCK_TIMEOUT} the statement fails, where a lock taken by mistake
 * in the wrong transaction would otherwise hang a request thread. The bound then holds for the rest of
 * the transaction, which only makes its other waits finite too.
 */
@Component
public class PostgresAccountLock implements AccountLockPort {

    /** Authentication's own space of advisory keys; the second key is the account. */
    static final int NAMESPACE = 1_001;
    static final String LOCK_TIMEOUT = "10s";

    private final JdbcClient jdbc;

    public PostgresAccountLock(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void lockFor(UUID userId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("An account lock lasts until its transaction ends: take it inside one");
        }
        jdbc.sql("SET LOCAL lock_timeout = '" + LOCK_TIMEOUT + "'").update();
        jdbc.sql("SELECT pg_advisory_xact_lock(:namespace, hashtext(:account))")
            .param("namespace", NAMESPACE)
            .param("account", userId.toString())
            .query((rs, rowNum) -> Boolean.TRUE)
            .single();
    }
}
