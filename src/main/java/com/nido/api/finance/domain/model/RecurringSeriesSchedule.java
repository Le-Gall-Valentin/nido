package com.nido.api.finance.domain.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A recurring series' scheduling fields alone — no label, no amount, no contributors.
 *
 * <p>Exists so a read path can answer "does this space owe any occurrence?" without paying
 * for the full series. Two things make that worth a separate shape rather than reusing
 * {@link RecurringTransactionSeries}:
 *
 * <p>Cost — the full series decrypts its label and amount, plus every contributor's share.
 * Every finance read used to pay that for every series just to discover nothing was due.
 *
 * <p>Correctness — the check runs before the materialization lock is taken, and whatever it
 * reads must not end up in the persistence context. A managed entity read before the lock
 * would be handed back unchanged by the read after it, since Hibernate keeps the instance it
 * already has and discards the freshly-read column values; a concurrent materialization that
 * committed in between would be invisible, and its occurrences inserted a second time. A
 * projection is not managed, so the read under the lock is a genuine first load.
 */
public record RecurringSeriesSchedule(
    UUID id,
    RecurrenceInterval intervalType,
    int intervalCount,
    LocalDate anchorDate,
    LocalDate endDate,
    LocalDate lastMaterializedDate
) {}
