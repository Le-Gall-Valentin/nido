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
 * Brings, at start, every encrypted value to the current format: the values of spaces sealed {@code v3:} with the space's
 * current key (SealedValueMigration), and the columns encrypted under a key of their own — two-factor secrets, SMTP
 * password, mail queue — to {@code k2:} (RekeyMigration). It starts from whatever an earlier version stored: in clear or
 * encrypted without an envelope (up to 0.13), sealed with the legacy key (0.14 to 0.15).
 *
 * <p>Runs once every singleton exists — after Liquibase and after the encryption key was decided — and before the
 * context's refresh ends, which is when the HTTP connector opens and the scheduled tasks start. So nothing reads a value
 * of an earlier format through code that refuses it. {@code @Lazy(false)}: a lazy context would otherwise never build
 * it. <b>No other start hook may read a sealed value</b>: they run in no set order with this one. The three stores
 * encrypted under a key of their own read both generations (CurrentOrLegacyTextEncryptor), for InstanceStartup reads
 * the settings at every start.
 *
 * <p>One instance at a time, under {@link SealingLock}: a second one starting at the same moment waits, then finds
 * nothing left. A failure stops the start, like a refused key: the rows rewritten before it stay so, and the next start
 * resumes. Once everything is current, this costs a few queries.
 *
 * <p>Keep it, with the legacy key (DataKeys.legacy, LegacySpaceOpener, the fallback of CurrentOrLegacyTextEncryptor) and
 * the modules' declarations, for as long as an installation may upgrade from 0.15.x or earlier straight to the current
 * version. To drop that support: remove the legacy key and its readers, and refuse at start a value that is not of the
 * current format, naming the last version that converts it.
 */
@Lazy(false)
@Component
public class EncryptionBackfillRunner implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(EncryptionBackfillRunner.class);

    private final List<SealedColumns> sealedColumns;
    private final List<RekeyedColumns> rekeyedColumns;
    private final List<ExistingCiphertextCheck> checks;
    private final SealedValueMigration migration;
    private final RekeyMigration rekeying;
    private final SpaceSealers sealers;
    private final LegacySpaceOpeners legacyOpeners;
    private final SealingLock lock;
    private final TableVacuum vacuum;
    private final PendingVacuum pendingVacuum;
    private final StartKey key;

    public EncryptionBackfillRunner(List<SealedColumns> sealedColumns, List<RekeyedColumns> rekeyedColumns,
                                    List<ExistingCiphertextCheck> checks, SealedValueMigration migration,
                                    RekeyMigration rekeying, SpaceSealers sealers, LegacySpaceOpeners legacyOpeners,
                                    SealingLock lock, TableVacuum vacuum, PendingVacuum pendingVacuum, StartKey key) {
        this.sealedColumns = sealedColumns;
        this.rekeyedColumns = rekeyedColumns;
        this.checks = checks;
        this.migration = migration;
        this.rekeying = rekeying;
        this.sealers = sealers;
        this.legacyOpeners = legacyOpeners;
        this.lock = lock;
        this.vacuum = vacuum;
        this.pendingVacuum = pendingVacuum;
        this.key = key;
    }

    @Override
    public void afterSingletonsInstantiated() {
        List<SealedColumn> columns = sealedColumns.stream().flatMap(declared -> declared.columns().stream()).toList();
        List<RekeyedColumn> rekeyed = rekeyedColumns.stream().flatMap(declared -> declared.columns().stream()).toList();
        // A key whose fingerprint the data has yet to confirm is only taken on the data's word: an installation older
        // than 0.12 never recorded one, and keeping the fingerprint of a wrong key would lock the right one out.
        boolean unconfirmedKey = key.awaitsConfirmation();
        if (!anythingPending(columns, rekeyed) && !pendingVacuum.anyOwed() && !unconfirmedKey) {
            return;
        }
        lock.whileHeld(() -> {
            // Another instance may have done the work while this one waited for the lock.
            boolean pending = anythingPending(columns, rekeyed);
            // Before anything is rewritten with the key: a start that only owes a VACUUM rewrites nothing.
            if (pending || unconfirmedKey) {
                verifyExistingCiphertext(unconfirmedKey);
            }
            if (pending) {
                // Every table declared, owed before the first row is rewritten: a start stopped from here on leaves
                // them to the next one, including tables it sealed whose columns in clear 068 then drops.
                Set<String> tables = new LinkedHashSet<>();
                columns.forEach(column -> tables.add(column.table()));
                rekeyed.forEach(column -> tables.add(column.table()));
                pendingVacuum.owe(tables);
                LegacySpaceOpeners remembered = LegacySpaceOpeners.remembering(legacyOpeners);
                for (SealedColumn column : columns) {
                    int rows = migration.migrate(column, sealers, remembered);
                    if (rows > 0) {
                        log.info("Sealed {} values of {} with the current key", rows, column);
                    }
                }
                for (RekeyedColumn column : rekeyed) {
                    int rows = rekeying.migrate(column);
                    if (rows > 0) {
                        log.info("Re-encrypted {} values of {} with the current key", rows, column);
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

    private boolean anythingPending(List<SealedColumn> columns, List<RekeyedColumn> rekeyed) {
        return columns.stream().anyMatch(migration::pending) || rekeyed.stream().anyMatch(rekeying::pending);
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
