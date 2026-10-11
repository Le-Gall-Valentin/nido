package com.nido.api.infrastructure.sealing;

import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * How the migrations rewrite a column — SealedValueMigration, RekeyMigration: in batches of {@value #SIZE} rows, one
 * transaction each, until a batch finds nothing left. A start that fails loses at most a batch, and the next one redoes
 * it. Its messages name the row and the column, never the value nor the message of a cause: a driver's can quote the
 * whole row ("Failing row contains …"), and a start that fails prints every cause it is given.
 */
final class BatchedRewrite {

    static final int SIZE = 500;

    private BatchedRewrite() {}

    /** @param batch rewrites up to {@value #SIZE} rows in the transaction it is given, and says what it did */
    static MigratedRows untilNoneLeft(TransactionTemplate transactions, Supplier<MigratedRows> batch) {
        MigratedRows done = MigratedRows.NONE;
        MigratedRows last;
        do {
            last = Objects.requireNonNull(transactions.execute(status -> batch.get()));
            done = done.plus(last);
        } while (last.total() > 0);
        return done;
    }

    /** Rows read and none changed: reading them again would loop for ever. */
    static void requireProgress(int read, MigratedRows changed, Object column, String stillThere) {
        if (read > 0 && changed.total() == 0) {
            throw new IllegalStateException(read + " rows of " + column + " " + stillThere);
        }
    }

    static IllegalStateException failure(String verb, Object row, Object column, RuntimeException cause) {
        return new IllegalStateException("Could not " + verb + " row " + row + " of " + column + " ("
            + cause.getClass().getSimpleName() + "). Its batch was rolled back; the rows done before it stay so, "
            + "and the next start resumes.");
    }

    static IllegalStateException wouldNotReadBack(String doing, Object row, Object column) {
        return new IllegalStateException(doing + " row " + row + " of " + column + " would not give its value back: "
            + "nothing of its batch was written.");
    }
}
