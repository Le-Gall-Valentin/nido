package com.nido.api.calendar.domain.port.out;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * What a space already holds of an iCalendar file: the UIDs it imported, and the rows a nido export named. Each list is
 * asked in one query, never split: a file holds at most 5,000 events, well under the 32,767 values a PostgreSQL query takes.
 */
public interface ImportedUidRepository {

    /** Among {@code hashes}, those an event or a series of the space was imported with. */
    Set<String> knownHashes(UUID spaceId, Collection<String> hashes);

    /** Among {@code ids}, those of an event or a series of the space. */
    Set<UUID> existingIds(UUID spaceId, Collection<UUID> ids);

    /**
     * Puts the imports of a space in line until the transaction ends: of two imports of one file at once, the second
     * finds what the first wrote, and passes it. Taken inside a transaction, before reading what the space holds.
     */
    void lockImports(UUID spaceId);
}
