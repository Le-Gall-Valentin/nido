package com.nido.api.infrastructure.config;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;

/**
 * Moves the values of a {@link PlaintextTable} from clear to encrypted, batch by batch. Through JDBC: the
 * entities no longer know the columns in clear.
 */
@Component
public class PlaintextTableEncryptor {

    /** Rows per transaction: a start that fails loses at most this much work, and the next one redoes it. */
    static final int BATCH_SIZE = 500;

    private final JdbcClient jdbc;
    private final TransactionTemplate transactions;

    public PlaintextTableEncryptor(JdbcClient jdbc, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    /** Whether a row still holds a value in clear — false for good once 068 dropped the columns. */
    public boolean pending(PlaintextTable table) {
        return hasColumnsInClear(table) && Boolean.TRUE.equals(jdbc.sql(
                "SELECT EXISTS (SELECT 1 FROM " + table.name() + " t WHERE " + anyInClear(table) + ")")
            .query(Boolean.class).single());
    }

    /**
     * Encrypts every value still in clear with the key of its row's space, and empties the clear column
     * in the same statement. A value in clear always wins: it is the latest the row was given, even over
     * an encrypted one already there.
     *
     * @return how many rows were rewritten
     */
    public int encrypt(PlaintextTable table, Function<UUID, TextEncryptor> keyOfSpace) {
        if (!hasColumnsInClear(table)) {
            return 0;
        }
        int rewritten = 0;
        int batch;
        do {
            batch = Objects.requireNonNull(transactions.execute(status -> encryptOneBatch(table, keyOfSpace)));
            rewritten += batch;
        } while (batch > 0);
        return rewritten;
    }

    private int encryptOneBatch(PlaintextTable table, Function<UUID, TextEncryptor> keyOfSpace) {
        List<RowInClear> rows = jdbc.sql("SELECT t.id, " + table.spaceOf() + " AS space_id, "
                + table.columns().stream().map(c -> "t." + c).collect(joining(", "))
                + " FROM " + table.name() + " t " + table.join()
                + " WHERE " + anyInClear(table) + " LIMIT " + BATCH_SIZE)
            .query((rs, rowNum) -> {
                Map<String, String> values = new LinkedHashMap<>();
                for (String column : table.columns()) {
                    values.put(column, rs.getString(column));
                }
                return new RowInClear(rs.getObject("id", UUID.class), rs.getObject("space_id", UUID.class), values);
            })
            .list();
        int rewritten = 0;
        for (RowInClear row : rows) {
            rewritten += rewrite(table, row, keyOfSpace);
        }
        if (rewritten == 0 && !rows.isEmpty()) {
            // Rows read in clear that would not change: looping would read them again forever.
            throw new IllegalStateException(rows.size() + " rows of " + table.name() + " stay in clear after being encrypted");
        }
        return rewritten;
    }

    private int rewrite(PlaintextTable table, RowInClear row, Function<UUID, TextEncryptor> keyOfSpace) {
        try {
            TextEncryptor encryptor = keyOfSpace.apply(row.spaceId());
            int changed = 0;
            for (Map.Entry<String, String> value : row.values().entrySet()) {
                if (value.getValue() == null) {
                    continue;
                }
                String column = value.getKey();
                changed += jdbc.sql("UPDATE " + table.name() + " SET " + column + "_encrypted = :encrypted, "
                        + column + " = NULL WHERE id = :id AND " + column + " IS NOT NULL")
                    .param("encrypted", encryptor.encrypt(value.getValue()))
                    .param("id", row.id())
                    .update();
            }
            return changed > 0 ? 1 : 0;
        } catch (RuntimeException e) {
            // Neither the value nor the cause: a driver's message can quote the whole row ("Failing row
            // contains ..."), and a start that fails prints every cause it is given.
            throw new IllegalStateException("Could not encrypt row " + row.id() + " of " + table.name()
                + " (" + e.getClass().getSimpleName() + "). Its batch was rolled back; the rows encrypted "
                + "before it stay encrypted, and the next start resumes.");
        }
    }

    private boolean hasColumnsInClear(PlaintextTable table) {
        long present = jdbc.sql("""
                SELECT count(*) FROM information_schema.columns
                WHERE table_schema = current_schema() AND table_name = :table AND column_name IN (:columns)""")
            .param("table", table.name())
            .param("columns", table.columns())
            .query(Long.class).single();
        if (present != 0 && present != table.columns().size()) {
            throw new IllegalStateException(table.name() + " has lost only some of its columns in clear");
        }
        return present != 0;
    }

    private static String anyInClear(PlaintextTable table) {
        return table.columns().stream().map(c -> "t." + c + " IS NOT NULL").collect(joining(" OR "));
    }

    private record RowInClear(UUID id, UUID spaceId, Map<String, String> values) {}
}
