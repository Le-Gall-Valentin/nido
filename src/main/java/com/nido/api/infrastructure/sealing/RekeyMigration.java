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

/**
 * Brings the values of a {@link RekeyedColumn} to the current key ({@value CurrentOrLegacyTextEncryptor#PREFIX}), in
 * batches, one transaction each — see BatchedRewrite. Each value is checked to open back to itself before it is
 * written, and written only if it is still the one read. A value no key opens stops the start, naming its row — or, for
 * a column that says so, has its row deleted. Never shows a value, nor the message of a cause that could hold one. A row
 * is found again by its key compared in the key's own type, so through its index.
 */
@Component
public class RekeyMigration {

    private static final Logger log = LoggerFactory.getLogger(RekeyMigration.class);

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

    /** @return the values re-encrypted, and — for a column that deletes them — those dropped as unreadable */
    public MigratedRows migrate(RekeyedColumn column) {
        return BatchedRewrite.untilNoneLeft(transactions, () -> migrateOneBatch(column));
    }

    private MigratedRows migrateOneBatch(RekeyedColumn column) {
        List<Row> rows = jdbc.sql("SELECT " + column.rowKey() + " AS row_key, " + column.column() + " AS stored FROM "
                + column.table() + " WHERE " + pendingCondition(column) + " LIMIT " + BatchedRewrite.SIZE)
            .query((rs, rowNum) -> new Row(rs.getObject("row_key"), rs.getString("stored")))
            .list();
        MigratedRows changed = MigratedRows.NONE;
        for (Row row : rows) {
            changed = changed.plus(rekey(column, row));
        }
        BatchedRewrite.requireProgress(rows.size(), changed, column, "stay under the legacy key after being re-encrypted");
        return changed;
    }

    private MigratedRows rekey(RekeyedColumn column, Row row) {
        TextEncryptor encryptor;
        String value;
        try {
            encryptor = column.encryptorFor(String.valueOf(row.key()));
        } catch (RuntimeException e) {
            throw BatchedRewrite.failure("re-encrypt", row.key(), column, e);
        }
        try {
            value = encryptor.decrypt(row.stored());
        } catch (RuntimeException e) {
            if (column.deletesUnreadable()) {
                log.error("Row {} of {} cannot be read with the key and is dropped: {}", row.key(), column, e.getClass().getSimpleName());
                return where(column, "DELETE FROM " + column.table(), row).update() > 0 ? MigratedRows.ONE_DROPPED : MigratedRows.NONE;
            }
            throw BatchedRewrite.failure("re-encrypt", row.key(), column, e);
        }
        String rekeyed;
        try {
            rekeyed = encryptor.encrypt(value);
        } catch (RuntimeException e) {
            throw BatchedRewrite.failure("re-encrypt", row.key(), column, e);
        }
        if (!CurrentOrLegacyTextEncryptor.isCurrent(rekeyed) || !value.equals(opened(encryptor, rekeyed))) {
            throw BatchedRewrite.wouldNotReadBack("Re-encrypting", row.key(), column);
        }
        try {
            return where(column, "UPDATE " + column.table() + " SET " + column.column() + " = :rekeyed", row)
                .param("rekeyed", rekeyed)
                .update() > 0 ? MigratedRows.ONE_REENCRYPTED : MigratedRows.NONE;
        } catch (RuntimeException e) {
            throw BatchedRewrite.failure("re-encrypt", row.key(), column, e);
        }
    }

    /** The row as it was read: its key, the rows of the column, and the value still the one read. */
    private JdbcClient.StatementSpec where(RekeyedColumn column, String statement, Row row) {
        return jdbc.sql(statement + " WHERE " + column.rowKey() + " = :key AND (" + column.rows() + ") AND "
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

    private static String pendingCondition(RekeyedColumn column) {
        return "(" + column.rows() + ") AND " + column.column() + " IS NOT NULL AND " + column.column()
            + " NOT LIKE '" + CurrentOrLegacyTextEncryptor.PREFIX + "%'";
    }

    /** The key as the driver reads it — a UUID, a string —, so that it is compared in its column's own type. */
    private record Row(Object key, String stored) {}
}
