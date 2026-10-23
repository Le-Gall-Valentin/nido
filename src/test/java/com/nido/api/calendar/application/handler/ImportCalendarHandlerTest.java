package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.service.CalendarSpaceMemberValidator;
import com.nido.api.calendar.domain.model.CalendarDigests;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.CreateEventCommand;
import com.nido.api.calendar.domain.model.CreateRecurringEventSeriesCommand;
import com.nido.api.calendar.domain.model.DayRule;
import com.nido.api.calendar.domain.model.ImportGroup;
import com.nido.api.calendar.domain.model.ImportNote;
import com.nido.api.calendar.domain.model.ImportNoteKind;
import com.nido.api.calendar.domain.model.ImportReading;
import com.nido.api.calendar.domain.model.ImportReport;
import com.nido.api.calendar.domain.model.ImportTarget;
import com.nido.api.calendar.domain.model.ImportedEvent;
import com.nido.api.calendar.domain.model.ImportedOccurrence;
import com.nido.api.calendar.domain.model.ImportedSeries;
import com.nido.api.calendar.domain.model.RecurrenceInterval;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.port.out.AccountLanguage;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.calendar.domain.port.out.EventExclusionRepository;
import com.nido.api.calendar.domain.port.out.IcalImportReader;
import com.nido.api.calendar.domain.port.out.ImportedUidRepository;
import com.nido.api.calendar.domain.port.out.RecurringEventSeriesRepository;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import com.nido.api.space.application.port.in.GetSpaceUseCase;
import com.nido.api.space.domain.model.SpaceDetailView;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportCalendarHandlerTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 13);

    private final IcalImportReader reader = mock(IcalImportReader.class);
    private final ImportedUidRepository importedUids = mock(ImportedUidRepository.class);
    private final CalendarEventRepository events = mock(CalendarEventRepository.class);
    private final RecurringEventSeriesRepository series = mock(RecurringEventSeriesRepository.class);
    private final EventExclusionRepository exclusions = mock(EventExclusionRepository.class);
    private final CalendarSpaceMemberValidator members = mock(CalendarSpaceMemberValidator.class);
    private final GetSpaceUseCase spaces = mock(GetSpaceUseCase.class);
    private final GetSpaceTodayUseCase today = mock(GetSpaceTodayUseCase.class);
    private final AccountLanguage language = mock(AccountLanguage.class);
    private final ImportCalendarHandler handler = new ImportCalendarHandler(reader, importedUids, events, series,
        exclusions, members, spaces, today, language);

    private final UUID spaceId = UUID.randomUUID();
    private final SpaceMembership member = new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());

    @BeforeEach
    void setUp() {
        when(spaces.get(spaceId, member)).thenReturn(new SpaceDetailView(spaceId, SpaceType.SHARED, "Chez nous", null,
            "#c17a5c", "🏡", PARIS, SpaceRole.MEMBER, 2));
        when(today.today(spaceId)).thenReturn(TODAY);
        when(language.of(member.userId())).thenReturn(Locale.FRENCH);
        when(members.participantsFor(member, List.of())).thenReturn(List.of());
        when(importedUids.knownHashes(eq(spaceId), anyCollection())).thenReturn(Set.of());
        when(importedUids.existingIds(eq(spaceId), anyCollection())).thenReturn(Set.of());
    }

    @Test
    void anAnalysisWritesNothingAndSaysWhatAnImportWould() {
        when(reader.read(eq("ics"), eq(PARIS), eq(TODAY), eq(Locale.FRENCH), any())).thenReturn(reading(event("a@test"), seriesGroup("b@test")));

        ImportReport report = handler.analyse("ics", member);

        assertThat(report.eventsCreated()).isEqualTo(1);
        assertThat(report.seriesCreated()).isEqualTo(1);
        assertThat(report.written()).isFalse();
        verify(events, never()).createImported(any(), any());
        verify(series, never()).createImported(any(), any());
    }

    @Test
    void anImportWritesEveryGroupWithTheDigestOfItsUid() {
        when(reader.read(eq("ics"), eq(PARIS), eq(TODAY), eq(Locale.FRENCH), any())).thenReturn(reading(event("a@test"), seriesGroup("b@test")));
        when(series.createImported(any(), any())).thenAnswer(call -> {
            CreateRecurringEventSeriesCommand command = call.getArgument(0);
            return new RecurringEventSeries(UUID.randomUUID(), spaceId, command.title(), null, null, true, null, null, 0,
                null, command.intervalType(), command.intervalCount(), command.weekdays(), command.dayRule(),
                command.anchorDate(), command.endDate(), command.startsOn(), List.of(), member.userId(), Instant.now());
        });

        ImportReport report = handler.importFile("ics", member);

        assertThat(report.written()).isTrue();
        verify(events).createImported(any(CreateEventCommand.class), eq(CalendarDigests.sha256Hex("a@test")));
        verify(series).createImported(any(CreateRecurringEventSeriesCommand.class), eq(CalendarDigests.sha256Hex("b@test")));
        verify(exclusions).exclude(any(), eq(LocalDate.of(2026, 1, 12)));
        // The edited occurrence: a detached event of the new series, imported with the same UID.
        verify(events, times(1)).createImported(argThat(c -> c.recurringSeriesId() != null),
            eq(CalendarDigests.sha256Hex("b@test")));
    }

    @Test
    void theReaderIsToldWhichUidsTheSpaceHoldsAlreadyAndTheirCountReachesTheReport() {
        when(importedUids.knownHashes(eq(spaceId), anyCollection())).thenReturn(Set.of(CalendarDigests.sha256Hex("a@test")));
        ImportReport[] report = new ImportReport[1];

        ImportTarget target = targetGiven(new ImportReading(List.of(event("c@test")), 1, 0, List.of()),
            () -> report[0] = handler.importFile("ics", member));

        assertThat(target.alreadyHere().apply(Set.of("a@test", "c@test"))).containsExactly("a@test");
        assertThat(report[0].duplicates()).isEqualTo(1);
        assertThat(report[0].eventsCreated()).isEqualTo(1);
        verify(events, never()).createImported(any(), eq(CalendarDigests.sha256Hex("a@test")));
    }

    @Test
    void aNidoExportOfThisSpaceIsRecognisedByItsIds() {
        UUID existing = UUID.randomUUID();
        when(importedUids.existingIds(eq(spaceId), anyCollection())).thenReturn(Set.of(existing));

        ImportTarget target = targetGiven(reading(), () -> handler.analyse("ics", member));

        assertThat(target.alreadyHere().apply(Set.of(existing + "@nido", UUID.randomUUID() + "@nido", "x@test")))
            .containsExactly(existing + "@nido");
    }

    @Test
    void theReaderIsToldTheRowsAnImportMayWriteAndWhoTakesPartInEachEntry() {
        assertThat(targetGiven(reading(), () -> handler.analyse("ics", member)))
            .extracting(ImportTarget::maxRows, ImportTarget::participantsPerEntry).containsExactly(10_000, 0);

        // In a personal space, its owner takes part in everything imported: one row more for each entry.
        when(members.participantsFor(member, List.of())).thenReturn(List.of(member.userId()));
        assertThat(targetGiven(reading(), () -> handler.analyse("ics", member)).participantsPerEntry()).isEqualTo(1);
    }

    @Test
    void aFileTheReaderFindsTooLargeWritesNothing() {
        when(reader.read(eq("ics"), eq(PARIS), eq(TODAY), eq(Locale.FRENCH), any()))
            .thenThrow(new CalendarException.ImportTooLarge(CalendarException.ImportTooLarge.Limit.ROWS, 10_000));

        assertThatThrownBy(() -> handler.importFile("ics", member)).isInstanceOf(CalendarException.ImportTooLarge.class);
        verify(events, never()).createImported(any(), any());
        verify(series, never()).createImported(any(), any());
    }

    @Test
    void aViewerCannotImportAndTheFileIsNeverRead() {
        SpaceMembership viewer = new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.VIEWER, Instant.now());
        assertThatThrownBy(() -> handler.analyse("ics", viewer)).isInstanceOf(SpaceException.InsufficientRole.class);
        assertThatThrownBy(() -> handler.importFile("ics", viewer)).isInstanceOf(SpaceException.InsufficientRole.class);
        verify(reader, never()).read(any(), any(), any(), any(), any());
    }

    @Test
    void anEventWithoutUidIsWrittenWithoutDigestAndNeverTakenForADuplicate() {
        when(reader.read(eq("ics"), eq(PARIS), eq(TODAY), eq(Locale.FRENCH), any())).thenReturn(reading(event(null), event(null)));

        ImportReport report = handler.importFile("ics", member);

        assertThat(report.eventsCreated()).isEqualTo(2);
        assertThat(report.duplicates()).isZero();
        verify(events, times(2)).createImported(any(CreateEventCommand.class), isNull());
    }

    @Test
    void unfoldedSeriesAndTheirOccurrencesAreCounted() {
        ImportGroup unfolded = new ImportGroup("u@test", List.of(imported(), imported(), imported()), null, true, 3);
        ImportGroup other = new ImportGroup("v@test", List.of(imported()), null, true, 1);
        when(reader.read(eq("ics"), eq(PARIS), eq(TODAY), eq(Locale.FRENCH), any())).thenReturn(reading(unfolded, other, event("a@test")));

        ImportReport report = handler.analyse("ics", member);

        assertThat(report.seriesUnfolded()).isEqualTo(2);
        assertThat(report.occurrencesUnfolded()).isEqualTo(4);
        assertThat(report.eventsCreated()).isEqualTo(5);
    }

    @Test
    void theReadersNotesAndItsCountOfIgnoredReachTheReport() {
        List<ImportNote> notes = List.of(new ImportNote(ImportNoteKind.IGNORED, "Copie", "DUPLICATE_UID"),
            new ImportNote(ImportNoteKind.IGNORED, "Boucle", "TOO_HEAVY"),
            new ImportNote(ImportNoteKind.IGNORED, "Illisible", "BAD_RULE"),
            new ImportNote(ImportNoteKind.IGNORED, "Tâche · Poubelles", "NIDO_SOURCE"));
        when(reader.read(eq("ics"), eq(PARIS), eq(TODAY), eq(Locale.FRENCH), any())).thenReturn(new ImportReading(List.of(event("a@test")), 0, 4, notes));

        ImportReport report = handler.analyse("ics", member);

        assertThat(report.ignored()).isEqualTo(4);
        assertThat(report.notes()).isEqualTo(notes);
    }

    @Test
    void aReportHoldsTheFirstTwoHundredNotes() {
        List<ImportNote> notes = IntStream.range(0, 250)
            .mapToObj(i -> new ImportNote(ImportNoteKind.TRUNCATED_TEXT, "Note " + i, "title")).toList();
        when(reader.read(eq("ics"), eq(PARIS), eq(TODAY), eq(Locale.FRENCH), any())).thenReturn(new ImportReading(List.of(), 0, 0, notes));

        assertThat(handler.analyse("ics", member).notes()).isEqualTo(notes.subList(0, 200));
    }

    @Test
    void aSeriesIsWrittenBeforeWhatHangsOnIt() {
        UUID createdId = UUID.randomUUID();
        when(reader.read(eq("ics"), eq(PARIS), eq(TODAY), eq(Locale.FRENCH), any())).thenReturn(reading(seriesGroup("b@test")));
        when(series.createImported(any(), any())).thenAnswer(call -> {
            CreateRecurringEventSeriesCommand command = call.getArgument(0);
            return new RecurringEventSeries(createdId, spaceId, command.title(), null, null, true, null, null, 0,
                null, command.intervalType(), command.intervalCount(), command.weekdays(), command.dayRule(),
                command.anchorDate(), command.endDate(), command.startsOn(), List.of(), member.userId(), Instant.now());
        });

        handler.importFile("ics", member);

        InOrder order = inOrder(series, exclusions, events);
        order.verify(series).createImported(any(), eq(CalendarDigests.sha256Hex("b@test")));
        order.verify(exclusions).exclude(createdId, LocalDate.of(2026, 1, 12));
        order.verify(events).createImported(argThat(c -> createdId.equals(c.recurringSeriesId())
            && LocalDate.of(2026, 1, 19).equals(c.recurringOriginalDate())), eq(CalendarDigests.sha256Hex("b@test")));
    }

    private static ImportReading reading(ImportGroup... groups) {
        return new ImportReading(List.of(groups), 0, 0, List.of());
    }

    /** The target the handler gave the reader, which reading "ics" answered with {@code reading}. */
    private ImportTarget targetGiven(ImportReading reading, Runnable call) {
        ArgumentCaptor<ImportTarget> target = ArgumentCaptor.forClass(ImportTarget.class);
        when(reader.read(eq("ics"), eq(PARIS), eq(TODAY), eq(Locale.FRENCH), target.capture())).thenReturn(reading);
        call.run();
        return target.getValue();
    }

    private static ImportGroup event(String uid) {
        return new ImportGroup(uid, List.of(imported()), null, false, 0);
    }

    private static ImportGroup seriesGroup(String uid) {
        return new ImportGroup(uid, List.of(), new ImportedSeries("Piano", null, null, true, null, null, 0,
            RecurrenceInterval.WEEKLY, 1, Set.of(DayOfWeek.MONDAY), DayRule.SAME_DAY, LocalDate.of(2026, 1, 5), null, null,
            Set.of(LocalDate.of(2026, 1, 12)), List.of(new ImportedOccurrence(LocalDate.of(2026, 1, 19), imported()))),
            false, 0);
    }

    private static ImportedEvent imported() {
        return new ImportedEvent("Match", null, null, true, LocalDate.of(2026, 10, 14), null, LocalDate.of(2026, 10, 14), null);
    }
}
