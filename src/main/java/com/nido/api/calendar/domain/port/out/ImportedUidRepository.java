package com.nido.api.calendar.domain.port.out;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/** What a space already holds of an iCalendar file: the UIDs it imported, and the rows a nido export named. */
public interface ImportedUidRepository {

    /** Among {@code hashes}, those an event or a series of the space was imported with. */
    Set<String> knownHashes(UUID spaceId, Collection<String> hashes);

    /** Among {@code ids}, those of an event or a series of the space. */
    Set<UUID> existingIds(UUID spaceId, Collection<UUID> ids);
}
