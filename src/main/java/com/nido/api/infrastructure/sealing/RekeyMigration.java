package com.nido.api.infrastructure.sealing;

import com.nido.api.infrastructure.encryption.CurrentOrLegacyTextEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Objects;

/**
 * Brings the values of a {@link RekeyedColumn} to the current key ({@value CurrentOrLegacyTextEncryptor#PREFIX}), in
 * batches, one transaction each. Each value is checked to open back to itself before it is written, and written only if
 * it is still the one read. A value no key opens stops the start, naming its row — or, for a column that says so, has its
 * row deleted. Never shows a value, nor the message of a cause that could hold one.
 */
@Component
public class RekeyMigration {

    private static final Logger log = LoggerFactory.getLogger(RekeyMigration.class);

    /** Rows per transaction: a start that fails loses at most this much work, and the next one redoes it. */
    static final int BATCH_SIZE = 500;

    private final JdbcClient jdbc;
    private final TransactionTemplate transactions;

    public RekeyMigration(JdbcClient jdbc, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    public boolean pending(RekeyedColumn column) {
        return Boolean.TRUE.equals(jdbc.sql("SELECT EXISTS (SELECT 1 FROM " + column.table() + " WHERE " + pendingCondition(column) + ")")
            .query(Boolean.class).single());
    }

    /** @return how many values were re-encrypted, or — for a column that deletes them — dropped as unreadable */
    public int migrate(RekeyedColumn column) {
        int done = 0;
        int batch;
        do {
            batch = Objects.requireNonNull(transactions.execute(status -> migrateOneBatch(column)));
            done += batch;
        } while (batch > 0);
        return done;
    }

    private int migrateOneBatch(RekeyedColumn column) {
        List<Row> rows = jdbc.sql("SELECT " + column.rowKey() + "::text AS row_key, " + column.column() + " AS stored FROM "
                + column.table() + " WHERE " + pendingCondition(column) + " LIMIT " + BATCH_SIZE)
            .query((rs, rowNum) -> new Row(rs.getString("row_key"), rs.getString("stored")))
            .list();
        int changed = 0;
        for (Row row : rows) {
            changed += rekey(column, row);
        }
        if (changed == 0 && !rows.isEmpty()) {
            // Rows that would not change: looping would read them again for ever.
            throw new IllegalStateException(rows.size() + " rows of " + column + " stay under the legacy key after being re-encrypted");
        }
        return changed;
    }

    private int rekey(RekeyedColumn column, Row row) {
        TextEncryptor encryptor;
        String value;
        try {
            encryptor = column.encryptorFor(row.key());
        } catch (RuntimeException e) {
            throw failure(column, row, e);
        }
        try {
            value = encryptor.decrypt(row.stored());
        } catch (RuntimeException e) {
            if (column.deletesUnreadable()) {
                log.error("Row {} of {} cannot be read with the key and is dropped: {}", row.key(), column, e.getClass().getSimpleName());
                return where(column, "DELETE FROM " + column.table(), row).update() > 0 ? 1 : 0;
            }
            throw failure(column, row, e);
        }
        String rekeyed;
        try {
            rekeyed = encryptor.encrypt(value);
        } catch (RuntimeException e) {
            throw failure(column, row, e);
        }
        if (!CurrentOrLegacyTextEncryptor.isCurrent(rekeyed) || !value.equals(opened(encryptor, rekeyed))) {
            throw new IllegalStateException("Re-encrypting row " + row.key() + " of " + column + " would not give its value back: "
                + "nothing of its batch was written.");
        }
        try {
            return where(column, "UPDATE " + column.table() + " SET " + column.column() + " = :rekeyed", row)
                .param("rekeyed", rekeyed)
                .update() > 0 ? 1 : 0;
        } catch (RuntimeException e) {
            throw failure(column, row, e);
        }
    }

    /** The row as it was read: its key, the rows of the column, and the value still the one read. */
    private JdbcClient.StatementSpec where(RekeyedColumn column, String statement, Row row) {
        return jdbc.sql(statement + " WHERE " + column.rowKey() + "::text = :key AND (" + column.rows() + ") AND "
                + column.column() + " = :old")
            .param("key", row.key())
            .param("old", row.stored());
    }

    private static String opened(TextEncryptor encryptor, String rekeyed) {
        try {
            return encryptor.decrypt(rekeyed);
        } catch (RuntimeException e) {
            return null;
        }
    }

    // Neither the value nor the cause: a driver's message can quote the whole row.
    private static IllegalStateException failure(RekeyedColumn column, Row row, RuntimeException e) {
        return new IllegalStateException("Could not re-encrypt row " + row.key() + " of " + column + " ("
            + e.getClass().getSimpleName() + "). Its batch was rolled back; the rows re-encrypted before it stay so, "
            + "and the next start resumes.");
    }

    private static String pendingCondition(RekeyedColumn column) {
        return "(" + column.rows() + ") AND " + column.column() + " IS NOT NULL AND " + column.column()
            + " NOT LIKE '" + CurrentOrLegacyTextEncryptor.PREFIX + "%'";
    }

    private record Row(String key, String stored) {}
}
