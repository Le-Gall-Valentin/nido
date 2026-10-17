package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.ImportCalendarUseCase;
import com.nido.api.calendar.application.service.CalendarSpaceMemberValidator;
import com.nido.api.calendar.domain.model.CalendarDigests;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.CreateEventCommand;
import com.nido.api.calendar.domain.model.CreateRecurringEventSeriesCommand;
import com.nido.api.calendar.domain.model.ImportGroup;
import com.nido.api.calendar.domain.model.ImportReading;
import com.nido.api.calendar.domain.model.ImportReport;
import com.nido.api.calendar.domain.model.ImportedEvent;
import com.nido.api.calendar.domain.model.ImportedOccurrence;
import com.nido.api.calendar.domain.model.ImportedSeries;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.port.out.AccountLanguage;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.calendar.domain.port.out.EventExclusionRepository;
import com.nido.api.calendar.domain.port.out.IcalImportReader;
import com.nido.api.calendar.domain.port.out.ImportedUidRepository;
import com.nido.api.calendar.domain.port.out.RecurringEventSeriesRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import com.nido.api.space.application.port.in.GetSpaceUseCase;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Imports an iCalendar file into a space. Nothing is kept between the analysis and the import: the import reads the file
 * again and writes it in one transaction — all of it, or nothing.
 *
 * <p>A UID already in the space — imported before, or the id of a row a nido export of this very space names — is
 * passed: what was changed here since is never overwritten. The calendar sends no notification, so a large import
 * disturbs nobody.
 */
@ApplicationService
public class ImportCalendarHandler implements ImportCalendarUseCase {

    static final int MAX_ROWS = 10_000;
    static final int MAX_NOTES = 200;
    private static final Pattern NIDO_UID = Pattern.compile("^([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})@nido$");

    private final IcalImportReader reader;
    private final ImportedUidRepository importedUids;
    private final CalendarEventRepository events;
    private final RecurringEventSeriesRepository series;
    private final EventExclusionRepository exclusions;
    private final CalendarSpaceMemberValidator members;
    private final GetSpaceUseCase spaces;
    private final GetSpaceTodayUseCase spaceToday;
    private final AccountLanguage language;

    public ImportCalendarHandler(IcalImportReader reader, ImportedUidRepository importedUids, CalendarEventRepository events,
                                 RecurringEventSeriesRepository series, EventExclusionRepository exclusions,
                                 CalendarSpaceMemberValidator members, GetSpaceUseCase spaces,
                                 GetSpaceTodayUseCase spaceToday, AccountLanguage language) {
        this.reader = reader;
        this.importedUids = importedUids;
        this.events = events;
        this.series = series;
        this.exclusions = exclusions;
        this.members = members;
        this.spaces = spaces;
        this.spaceToday = spaceToday;
        this.language = language;
    }

    @Override
    @Transactional(readOnly = true)
    public ImportReport analyse(String content, SpaceMembership caller) {
        return run(content, caller, false);
    }

    @Override
    @Transactional
    public ImportReport importFile(String content, SpaceMembership caller) {
        return run(content, caller, true);
    }

    private ImportReport run(String content, SpaceMembership caller, boolean write) {
        caller.ensureCanWrite();
        ImportReading reading = reader.read(content, spaces.get(caller.spaceId(), caller).timezone(),
            spaceToday.today(caller.spaceId()), language.of(caller.userId()));

        List<String> hashes = reading.groups().stream().map(ImportGroup::uid).filter(uid -> uid != null)
            .map(CalendarDigests::sha256Hex).toList();
        Set<String> known = importedUids.knownHashes(caller.spaceId(), hashes);
        List<UUID> nidoIds = reading.groups().stream().map(group -> nidoId(group.uid())).flatMap(Optional::stream).toList();
        Set<UUID> existing = importedUids.existingIds(caller.spaceId(), nidoIds);
        List<ImportGroup> fresh = reading.groups().stream()
            .filter(group -> group.uid() == null
                || !known.contains(CalendarDigests.sha256Hex(group.uid()))
                    && nidoId(group.uid()).filter(existing::contains).isEmpty())
            .toList();
        if (fresh.stream().mapToInt(ImportGroup::rows).sum() > MAX_ROWS) {
            throw new CalendarException.ImportTooLarge("more than " + MAX_ROWS + " rows to write");
        }

        if (write) {
            List<UUID> participants = members.participantsFor(caller, List.of());
            fresh.forEach(group -> write(group, caller, participants));
        }
        return new ImportReport(
            fresh.stream().mapToInt(group -> group.events().size()).sum(),
            (int) fresh.stream().filter(group -> group.series() != null).count(),
            (int) fresh.stream().filter(ImportGroup::unfolded).count(),
            fresh.stream().mapToInt(ImportGroup::unfoldedOccurrences).sum(),
            reading.groups().size() - fresh.size(),
            reading.ignored(),
            reading.notes().stream().limit(MAX_NOTES).toList(),
            write);
    }

    private void write(ImportGroup group, SpaceMembership caller, List<UUID> participants) {
        String hash = group.uid() == null ? null : CalendarDigests.sha256Hex(group.uid());
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
                                                   UUID seriesId, java.time.LocalDate slot) {
        return new CreateEventCommand(caller.spaceId(), event.title(), event.description(), event.location(),
            event.allDay(), event.startDate(), event.startTime(), event.endDate(), event.endTime(), null, participants,
            seriesId, slot, caller.userId());
    }

    /** The id a UID of a nido export names: "<uuid>@nido". */
    private static Optional<UUID> nidoId(String uid) {
        if (uid == null) {
            return Optional.empty();
        }
        Matcher matcher = NIDO_UID.matcher(uid);
        return matcher.matches() ? Optional.of(UUID.fromString(matcher.group(1))) : Optional.empty();
    }
}
