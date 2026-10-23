package com.nido.api.calendar.infrastructure.persistence.adapter;

import com.nido.api.calendar.domain.port.out.ImportedUidRepository;
import com.nido.api.calendar.infrastructure.persistence.repository.CalendarEventJpaRepository;
import com.nido.api.calendar.infrastructure.persistence.repository.CalendarRecurringEventSeriesJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
public class ImportedUidRepositoryAdapter implements ImportedUidRepository {

    private final CalendarEventJpaRepository events;
    private final CalendarRecurringEventSeriesJpaRepository series;
    private final CalendarAdvisoryLocks locks;

    public ImportedUidRepositoryAdapter(CalendarEventJpaRepository events, CalendarRecurringEventSeriesJpaRepository series,
                                        CalendarAdvisoryLocks locks) {
        this.events = events;
        this.series = series;
        this.locks = locks;
    }

    @Override
    public void lockImports(UUID spaceId) {
        locks.lock("imports:" + spaceId);
    }

    @Override
    public Set<String> knownHashes(UUID spaceId, Collection<String> hashes) {
        if (hashes.isEmpty()) {
            return Set.of();
        }
        Set<String> known = new HashSet<>(events.findImportHashes(spaceId, hashes));
        known.addAll(series.findImportHashes(spaceId, hashes));
        return known;
    }

    @Override
    public Set<UUID> existingIds(UUID spaceId, Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Set.of();
        }
        Set<UUID> existing = new HashSet<>(events.findIdsIn(spaceId, ids));
        existing.addAll(series.findIdsIn(spaceId, ids));
        return existing;
    }
}
