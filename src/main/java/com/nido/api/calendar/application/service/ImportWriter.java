package com.nido.api.calendar.application.service;

import com.nido.api.calendar.domain.model.CalendarDigests;
import com.nido.api.calendar.domain.model.CreateEventCommand;
import com.nido.api.calendar.domain.model.CreateRecurringEventSeriesCommand;
import com.nido.api.calendar.domain.model.EventScheduleValidator;
import com.nido.api.calendar.domain.model.ImportGroup;
import com.nido.api.calendar.domain.model.ImportedEvent;
import com.nido.api.calendar.domain.model.ImportedOccurrence;
import com.nido.api.calendar.domain.model.ImportedSeries;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.calendar.domain.port.out.EventExclusionRepository;
import com.nido.api.calendar.domain.port.out.ImportedUidRepository;
import com.nido.api.calendar.domain.port.out.RecurringEventSeriesRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Writes what an import read, in one transaction: all of it, or nothing. The file was read before, outside any
 * transaction — a reading may take seconds, which no connection is held for. So the imports of a space take turns here,
 * and a UID another import wrote since the reading is passed as one the space holds already.
 */
@ApplicationService
public class ImportWriter {

    private final ImportedUidRepository importedUids;
    private final KnownImportUids known;
    private final CalendarEventRepository events;
    private final RecurringEventSeriesRepository series;
    private final EventExclusionRepository exclusions;

    public ImportWriter(ImportedUidRepository importedUids, KnownImportUids known, CalendarEventRepository events,
                        RecurringEventSeriesRepository series, EventExclusionRepository exclusions) {
        this.importedUids = importedUids;
        this.known = known;
        this.events = events;
        this.series = series;
        this.exclusions = exclusions;
    }

    /** Writes the groups the space does not hold yet, and returns them. */
    @Transactional
    public List<ImportGroup> write(List<ImportGroup> groups, SpaceMembership caller, List<UUID> participants) {
        importedUids.lockImports(caller.spaceId());
        Set<String> uids = groups.stream().map(ImportGroup::uid).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> here = uids.isEmpty() ? Set.of() : known.amongThese(caller.spaceId(), uids);
        List<ImportGroup> fresh = groups.stream().filter(group -> group.uid() == null || !here.contains(group.uid())).toList();
        // The reader keeps only what nido can hold: checked again here, as every other write of an event is.
        fresh.forEach(ImportWriter::validate);
        fresh.forEach(group -> write(group, group.uid() == null ? null : CalendarDigests.sha256Hex(group.uid()), caller,
            participants));
        return fresh;
    }

    private static void validate(ImportGroup group) {
        group.events().forEach(ImportWriter::validate);
        ImportedSeries imported = group.series();
        if (imported != null) {
            EventScheduleValidator.validateSeries(imported.allDay(), imported.startTime(), imported.endTime(),
                imported.durationDays(), imported.intervalType(), imported.intervalCount(), imported.weekdays(),
                imported.dayRule(), imported.anchorDate(), imported.endDate());
            imported.editedOccurrences().forEach(edited -> validate(edited.event()));
        }
    }

    private static void validate(ImportedEvent event) {
        EventScheduleValidator.validateEvent(event.allDay(), event.startDate(), event.startTime(), event.endDate(),
            event.endTime());
    }

    private void write(ImportGroup group, String hash, SpaceMembership caller, List<UUID> participants) {
        for (ImportedEvent event : group.events()) {
            events.createImported(eventCommand(event, caller, participants, null, null), hash);
        }
        ImportedSeries imported = group.series();
        if (imported == null) {
            return;
        }
        RecurringEventSeries created = series.createImported(new CreateRecurringEventSeriesCommand(caller.spaceId(),
            imported.title(), imported.description(), imported.location(), imported.allDay(), imported.startTime(),
            imported.endTime(), imported.durationDays(), null, imported.intervalType(), imported.intervalCount(),
            imported.weekdays(), imported.dayRule(), imported.anchorDate(), imported.endDate(), participants,
            caller.userId(), imported.startsOn()), hash);
        imported.cancelledSlots().forEach(slot -> exclusions.exclude(created.id(), slot));
        for (ImportedOccurrence edited : imported.editedOccurrences()) {
            events.createImported(eventCommand(edited.event(), caller, participants, created.id(), edited.slot()), hash);
        }
    }

    private static CreateEventCommand eventCommand(ImportedEvent event, SpaceMembership caller, List<UUID> participants,
                                                   UUID seriesId, LocalDate slot) {
        return new CreateEventCommand(caller.spaceId(), event.title(), event.description(), event.location(),
            event.allDay(), event.startDate(), event.startTime(), event.endDate(), event.endTime(), null, participants,
            seriesId, slot, caller.userId());
    }
}
