package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.ImportCalendarUseCase;
import com.nido.api.calendar.application.service.CalendarSpaceMemberValidator;
import com.nido.api.calendar.application.service.ImportWriter;
import com.nido.api.calendar.application.service.KnownImportUids;
import com.nido.api.calendar.domain.model.ImportGroup;
import com.nido.api.calendar.domain.model.ImportReading;
import com.nido.api.calendar.domain.model.ImportReport;
import com.nido.api.calendar.domain.model.ImportTarget;
import com.nido.api.calendar.domain.port.out.AccountLanguage;
import com.nido.api.calendar.domain.port.out.IcalImportReader;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import com.nido.api.space.application.port.in.GetSpaceUseCase;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.List;
import java.util.UUID;

/**
 * Imports an iCalendar file into a space. Nothing is kept between the analysis and the import: the import reads the file
 * again, outside any transaction, then writes it in one (ImportWriter) — all of it, or nothing.
 *
 * <p>A UID already in the space — imported before, or the id of a row a nido export of this very space names — is
 * passed: what was changed here since is never overwritten. A task, a meal, finance or savings a nido export wrote is
 * never imported, in any space: the reader sets them aside. The calendar sends no notification, so a large import
 * disturbs nobody.
 */
@ApplicationService
public class ImportCalendarHandler implements ImportCalendarUseCase {

    static final int MAX_ROWS = 10_000;
    /** The notes a report holds at most. The frontend says so from 200 on: keep MAX_IMPORT_NOTES (entities/calendar) equal. */
    static final int MAX_NOTES = 200;

    private final IcalImportReader reader;
    private final KnownImportUids known;
    private final ImportWriter writer;
    private final CalendarSpaceMemberValidator members;
    private final GetSpaceUseCase spaces;
    private final GetSpaceTodayUseCase spaceToday;
    private final AccountLanguage language;

    public ImportCalendarHandler(IcalImportReader reader, KnownImportUids known, ImportWriter writer,
                                 CalendarSpaceMemberValidator members, GetSpaceUseCase spaces,
                                 GetSpaceTodayUseCase spaceToday, AccountLanguage language) {
        this.reader = reader;
        this.known = known;
        this.writer = writer;
        this.members = members;
        this.spaces = spaces;
        this.spaceToday = spaceToday;
        this.language = language;
    }

    @Override
    public ImportReport analyse(String content, SpaceMembership caller) {
        caller.ensureCanWrite();
        List<UUID> participants = members.participantsFor(caller, List.of());
        ImportReading reading = read(content, caller, participants);
        return report(reading, reading.groups(), 0, false);
    }

    @Override
    public ImportReport importFile(String content, SpaceMembership caller) {
        caller.ensureCanWrite();
        List<UUID> participants = members.participantsFor(caller, List.of());
        ImportReading reading = read(content, caller, participants);
        List<ImportGroup> written = writer.write(reading.groups(), caller, participants);
        return report(reading, written, reading.groups().size() - written.size(), true);
    }

    private ImportReading read(String content, SpaceMembership caller, List<UUID> participants) {
        return reader.read(content, spaces.get(caller.spaceId(), caller).timezone(), spaceToday.today(caller.spaceId()),
            language.of(caller.userId()),
            new ImportTarget(uids -> known.amongThese(caller.spaceId(), uids), MAX_ROWS, participants.size()));
    }

    /** {@code passedMeanwhile}: groups another import of the space wrote between the reading and the writing. */
    private static ImportReport report(ImportReading reading, List<ImportGroup> groups, int passedMeanwhile, boolean written) {
        return new ImportReport(
            groups.stream().mapToInt(group -> group.events().size()).sum(),
            (int) groups.stream().filter(group -> group.series() != null).count(),
            (int) groups.stream().filter(ImportGroup::unfolded).count(),
            groups.stream().mapToInt(ImportGroup::unfoldedOccurrences).sum(),
            reading.alreadyImported() + passedMeanwhile,
            reading.ignored(),
            reading.notes().stream().limit(MAX_NOTES).toList(),
            written);
    }
}
