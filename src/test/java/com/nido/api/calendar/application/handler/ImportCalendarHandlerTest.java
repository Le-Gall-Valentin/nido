package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.service.CalendarSpaceMemberValidator;
import com.nido.api.calendar.domain.model.CalendarDigests;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.CreateEventCommand;
import com.nido.api.calendar.domain.model.CreateRecurringEventSeriesCommand;
import com.nido.api.calendar.domain.model.DayRule;
import com.nido.api.calendar.domain.model.ImportGroup;
import com.nido.api.calendar.domain.model.ImportReading;
import com.nido.api.calendar.domain.model.ImportReport;
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

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
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
        when(reader.read("ics", PARIS, TODAY, Locale.FRENCH)).thenReturn(reading(event("a@test"), seriesGroup("b@test")));

        ImportReport report = handler.analyse("ics", member);

        assertThat(report.eventsCreated()).isEqualTo(1);
        assertThat(report.seriesCreated()).isEqualTo(1);
        assertThat(report.written()).isFalse();
        verify(events, never()).createImported(any(), any());
        verify(series, never()).createImported(any(), any());
    }

    @Test
    void anImportWritesEveryGroupWithTheDigestOfItsUid() {
        when(reader.read("ics", PARIS, TODAY, Locale.FRENCH)).thenReturn(reading(event("a@test"), seriesGroup("b@test")));
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
        verify(events, times(1)).createImported(org.mockito.ArgumentMatchers.argThat(c -> c.recurringSeriesId() != null),
            eq(CalendarDigests.sha256Hex("b@test")));
    }

    @Test
    void aUidAlreadyImportedInTheSpaceIsPassed() {
        when(reader.read("ics", PARIS, TODAY, Locale.FRENCH)).thenReturn(reading(event("a@test"), event("c@test")));
        when(importedUids.knownHashes(eq(spaceId), anyCollection())).thenReturn(Set.of(CalendarDigests.sha256Hex("a@test")));

        ImportReport report = handler.importFile("ics", member);

        assertThat(report.duplicates()).isEqualTo(1);
        assertThat(report.eventsCreated()).isEqualTo(1);
        verify(events, never()).createImported(any(), eq(CalendarDigests.sha256Hex("a@test")));
    }

    @Test
    void aNidoExportOfThisSpaceIsRecognisedByItsIds() {
        UUID existing = UUID.randomUUID();
        when(reader.read("ics", PARIS, TODAY, Locale.FRENCH)).thenReturn(reading(event(existing + "@nido")));
        when(importedUids.existingIds(eq(spaceId), anyCollection())).thenReturn(Set.of(existing));

        assertThat(handler.importFile("ics", member).duplicates()).isEqualTo(1);
        verify(events, never()).createImported(any(), any());
    }

    @Test
    void moreThanTenThousandRowsIsTooMuch() {
        List<ImportedEvent> many = Collections.nCopies(10_001, imported());
        when(reader.read("ics", PARIS, TODAY, Locale.FRENCH)).thenReturn(
            new ImportReading(List.of(new ImportGroup("x@test", many, null, true, 10_001)), 0, List.of()));

        assertThatThrownBy(() -> handler.analyse("ics", member)).isInstanceOf(CalendarException.ImportTooLarge.class);
    }

    @Test
    void cancelledSlotsCountAmongTheRowsOfAnImport() {
        Set<LocalDate> slots = new java.util.HashSet<>();
        for (int i = 0; i < 10_001; i++) {
            slots.add(LocalDate.of(2026, 1, 12).plusDays(i));
        }
        ImportGroup group = new ImportGroup("x@test", List.of(), new ImportedSeries("Piano", null, null, true, null, null, 0,
            RecurrenceInterval.DAILY, 1, null, DayRule.SAME_DAY, LocalDate.of(2026, 1, 5), null, null, slots, List.of()),
            false, 0);
        when(reader.read("ics", PARIS, TODAY, Locale.FRENCH)).thenReturn(reading(group));

        assertThatThrownBy(() -> handler.importFile("ics", member)).isInstanceOf(CalendarException.ImportTooLarge.class);
        assertThatThrownBy(() -> handler.analyse("ics", member)).isInstanceOf(CalendarException.ImportTooLarge.class);
        verify(events, never()).createImported(any(), any());
        verify(series, never()).createImported(any(), any());
        verify(exclusions, never()).exclude(any(), any());
    }

    @Test
    void aViewerCannotImport() {
        SpaceMembership viewer = new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.VIEWER, Instant.now());
        assertThatThrownBy(() -> handler.analyse("ics", viewer)).isInstanceOf(SpaceException.InsufficientRole.class);
    }

    private static ImportReading reading(ImportGroup... groups) {
        return new ImportReading(List.of(groups), 0, List.of());
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
