package com.nido.api.infrastructure.sealing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Seals, at start, what versions before 0.14.0 stored: values in clear and values encrypted without an envelope —
 * see {@link SealedValueMigration}.
 *
 * <p>Runs once every singleton exists — after Liquibase and after the encryption key was decided — and
 * before the context's refresh ends, which is when the HTTP connector opens and the scheduled tasks
 * start. So nothing reads a value still in clear or in the format before through code that expects it sealed.
 * {@code @Lazy(false)}: a lazy context would otherwise never build it, and serve the old values to code that refuses
 * them. <b>No other start hook may read a sealed value</b>: they run in no set order with this one, and a value read
 * before it would still be in clear or in the format before 0.14.0.
 *
 * <p>One instance at a time, under {@link SealingLock}: a second one starting at the same moment waits, then finds
 * nothing left. A failure stops the start, like a refused key: the rows sealed before it stay sealed, and the
 * next start resumes. Once everything is sealed, this costs a few queries.
 *
 * <p>Keep it, with the modules' declarations, for as long as an installation may upgrade from 0.13.0 or earlier
 * straight to the current version: without it, 067 would add the encrypted columns, 068 would wait for ever, and
 * the adapters would refuse every value they read.
 */
@Lazy(false)
@Component
public class EncryptionBackfillRunner implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(EncryptionBackfillRunner.class);

    private final List<SealedColumns> sealedColumns;
    private final List<ExistingCiphertextCheck> checks;
    private final SealedValueMigration migration;
    private final SpaceSealers sealers;
    private final SealingLock lock;
    private final TableVacuum vacuum;
    private final PendingVacuum pendingVacuum;
    private final StartKey key;

    public EncryptionBackfillRunner(List<SealedColumns> sealedColumns, List<ExistingCiphertextCheck> checks,
                                    SealedValueMigration migration, SpaceSealers sealers, SealingLock lock,
                                    TableVacuum vacuum, PendingVacuum pendingVacuum, StartKey key) {
        this.sealedColumns = sealedColumns;
        this.checks = checks;
        this.migration = migration;
        this.sealers = sealers;
        this.lock = lock;
        this.vacuum = vacuum;
        this.pendingVacuum = pendingVacuum;
        this.key = key;
    }

    @Override
    public void afterSingletonsInstantiated() {
        List<SealedColumn> columns = sealedColumns.stream().flatMap(declared -> declared.columns().stream()).toList();
        // A key whose fingerprint the data has yet to confirm is only taken on the data's word: an installation older
        // than 0.12 never recorded one, and keeping the fingerprint of a wrong key would lock the right one out.
        boolean unconfirmedKey = key.awaitsConfirmation();
        if (!anythingPending(columns) && !pendingVacuum.anyOwed() && !unconfirmedKey) {
            return;
        }
        lock.whileHeld(() -> {
            // Another instance may have done the work while this one waited for the lock.
            boolean pending = anythingPending(columns);
            // Before anything is rewritten with the key: a start that only owes a VACUUM rewrites nothing.
            if (pending || unconfirmedKey) {
                verifyExistingCiphertext(unconfirmedKey);
            }
            if (pending) {
                // Every table declared, owed before the first row is rewritten: a start stopped from here on leaves
                // them to the next one, including tables it sealed whose columns in clear 068 then drops.
                Set<String> tables = new LinkedHashSet<>();
                columns.forEach(column -> tables.add(column.table()));
                pendingVacuum.owe(tables);
                for (SealedColumn column : columns) {
                    int rows = migration.migrate(column, sealers);
                    if (rows > 0) {
                        log.info("Sealed {} values of {} that earlier versions stored", rows, column);
                    }
                }
            }
            for (String table : pendingVacuum.stillOwed()) {
                if (vacuum.vacuumFull(table)) {
                    pendingVacuum.settle(table);
                }
            }
        });
    }

    private boolean anythingPending(List<SealedColumn> columns) {
        return columns.stream().anyMatch(migration::pending);
    }

    private void verifyExistingCiphertext(boolean unconfirmedKey) {
        try {
            checks.forEach(ExistingCiphertextCheck::verify);
        } catch (RuntimeException refused) {
            if (unconfirmedKey) {
                key.forget();
            }
            throw refused;
        }
        if (unconfirmedKey) {
            key.confirm();
        }
    }
}
