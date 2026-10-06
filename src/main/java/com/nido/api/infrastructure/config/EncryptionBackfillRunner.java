package com.nido.api.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Encrypts, at start, what versions before 0.13.1 stored in clear.
 *
 * <p>Runs once every singleton exists — after Liquibase and after the encryption key was decided — and
 * before the context's refresh ends, which is when the HTTP connector opens and the scheduled tasks
 * start. So nothing reads a row still in clear through code that expects it encrypted.
 *
 * <p>A failure stops the start, like a refused key: the rows encrypted before it stay encrypted, and the
 * next start resumes. Once 068 has dropped the columns in clear, nothing is pending, and this costs a
 * few catalog queries.
 */
@Component
public class EncryptionBackfillRunner implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(EncryptionBackfillRunner.class);

    private final List<EncryptionBackfill> backfills;
    private final List<ExistingCiphertextCheck> checks;
    private final TableVacuum vacuum;

    public EncryptionBackfillRunner(List<EncryptionBackfill> backfills, List<ExistingCiphertextCheck> checks,
                                    TableVacuum vacuum) {
        this.backfills = backfills;
        this.checks = checks;
        this.vacuum = vacuum;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (backfills.stream().noneMatch(EncryptionBackfill::pending)) {
            return;
        }
        checks.forEach(ExistingCiphertextCheck::verify);
        Set<String> rewritten = new LinkedHashSet<>();
        for (EncryptionBackfill backfill : backfills) {
            backfill.run().forEach((table, rows) -> {
                if (rows > 0) {
                    log.info("Encrypted {} rows of {} that earlier versions stored in clear", rows, table);
                    rewritten.add(table);
                }
            });
        }
        vacuum.vacuumFull(rewritten);
    }
}
