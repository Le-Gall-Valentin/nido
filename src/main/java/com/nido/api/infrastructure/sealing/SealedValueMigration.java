package com.nido.api.infrastructure.sealing;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Brings every value of a sealed column to the current format: {@code v3:}, sealed with the space's current key. It
 * starts from whatever an earlier version stored — in clear (up to 0.13), encrypted without an envelope (up to 0.13), or
 * sealed with the legacy key ({@code v2:}, 0.14 to 0.15) — column by column, in batches. Each value is checked to open
 * back to itself before it is written, so a sealing that would not read back stops the start before its batch is
 * written. Through JDBC: the entities no longer know the columns in clear.
 */
@Component
public class SealedValueMigration {

    /** Rows per transaction: a start that fails loses at most this much work, and the next one redoes it. */
    static final int BATCH_SIZE = 500;

    private final JdbcClient jdbc;
    private final TransactionTemplate transactions;

    public SealedValueMigration(JdbcClient jdbc, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    public boolean pending(SealedColumn column) {
        return Boolean.TRUE.equals(jdbc.sql("SELECT EXISTS (SELECT 1 FROM " + column.table() + " t WHERE "
                + pendingCondition(column, hasClearColumn(column)) + ")")
            .query(Boolean.class).single());
    }

    /** @return how many rows were sealed */
    public int migrate(SealedColumn column, SpaceSealers sealers, LegacySpaceOpeners legacy) {
        int sealed = 0;
        int batch;
        do {
            batch = Objects.requireNonNull(transactions.execute(status -> migrateOneBatch(column, sealers, legacy)));
            sealed += batch;
        } while (batch > 0);
        return sealed;
    }

    private int migrateOneBatch(SealedColumn column, SpaceSealers sealers, LegacySpaceOpeners legacy) {
        boolean withClear = hasClearColumn(column);
        List<Row> rows = jdbc.sql("SELECT t.id, " + column.spaceOf() + " AS space_id, t." + column.column() + " AS sealed"
                + (withClear ? ", t." + column.clearColumn().orElseThrow() + " AS clear" : "")
                + " FROM " + column.table() + " t " + column.join()
                + " WHERE " + pendingCondition(column, withClear) + " LIMIT " + BATCH_SIZE)
            .query((rs, rowNum) -> new Row(rs.getObject("id", UUID.class), rs.getObject("space_id", UUID.class),
                rs.getString("sealed"), withClear ? rs.getString("clear") : null))
            .list();
        int rewritten = 0;
        for (Row row : rows) {
            rewritten += seal(column, withClear, row, sealers, legacy);
        }
        if (rewritten == 0 && !rows.isEmpty()) {
            // Rows that would not change: looping would read them again for ever.
            throw new IllegalStateException(rows.size() + " rows of " + column + " stay unsealed after being sealed");
        }
        return rewritten;
    }

    private int seal(SealedColumn column, boolean withClear, Row row, SpaceSealers sealers, LegacySpaceOpeners legacy) {
        SpaceSealer sealer;
        String value;
        String sealed;
        try {
            sealer = sealers.forSpace(row.spaceId());
            value = row.clear() != null ? row.clear() : legacy.forSpace(row.spaceId()).open(column, row.id(), row.sealed());
            sealed = sealer.seal(column, row.id(), value);
        } catch (RuntimeException e) {
            throw failure(column, row, e);
        }
        if (!opensBackTo(sealer, column, row.id(), sealed, value)) {
            throw new IllegalStateException("Sealing row " + row.id() + " of " + column + " would not give its value back: "
                + "nothing of its batch was written.");
        }
        try {
            String clear = column.clearColumn().orElse(null);
            return jdbc.sql("UPDATE " + column.table() + " SET " + column.column() + " = :sealed"
                    + (withClear ? ", " + clear + " = NULL" : "")
                    + " WHERE id = :id AND " + column.column() + " IS NOT DISTINCT FROM CAST(:old AS text)"
                    + (withClear ? " AND " + clear + " IS NOT DISTINCT FROM CAST(:oldClear AS text)" : ""))
                .param("sealed", sealed)
                .param("id", row.id())
                .param("old", row.sealed())
                .param("oldClear", row.clear())
                .update() > 0 ? 1 : 0;
        } catch (RuntimeException e) {
            throw failure(column, row, e);
        }
    }

    private static boolean opensBackTo(SpaceSealer sealer, SealedColumn column, UUID rowId, String sealed, String value) {
        try {
            return value.equals(sealer.open(column, rowId, sealed));
        } catch (SealedValueRejected e) {
            return false;
        }
    }

    // Neither the value nor the cause: a driver's message can quote the whole row ("Failing row contains ..."), and a
    // start that fails prints every cause it is given.
    private static IllegalStateException failure(SealedColumn column, Row row, RuntimeException e) {
        return new IllegalStateException("Could not seal row " + row.id() + " of " + column + " ("
            + e.getClass().getSimpleName() + "). Its batch was rolled back; the rows sealed before it stay sealed, "
            + "and the next start resumes.");
    }

    /** Whether the column that held its values in clear up to 0.13 is still there: 068 drops it at the start after. */
    public boolean clearColumnRemains(SealedColumn column) {
        return hasClearColumn(column);
    }

    private boolean hasClearColumn(SealedColumn column) {
        return column.clearColumn().map(clear -> Boolean.TRUE.equals(jdbc.sql("""
                SELECT EXISTS (SELECT 1 FROM information_schema.columns
                               WHERE table_schema = current_schema() AND table_name = :table AND column_name = :column)""")
            .param("table", column.table()).param("column", clear)
            .query(Boolean.class).single())).orElse(false);
    }

    private static String pendingCondition(SealedColumn column, boolean withClear) {
        String unsealed = "(t." + column.column() + " IS NOT NULL AND t." + column.column() + " NOT LIKE '" + SpaceSealer.PREFIX + "%')";
        return withClear ? "(t." + column.clearColumn().orElseThrow() + " IS NOT NULL OR " + unsealed + ")" : unsealed;
    }

    private record Row(UUID id, UUID spaceId, String sealed, String clear) {}
}
