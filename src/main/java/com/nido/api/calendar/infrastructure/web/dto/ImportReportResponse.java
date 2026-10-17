package com.nido.api.calendar.infrastructure.web.dto;

import com.nido.api.calendar.domain.model.ImportNoteKind;
import com.nido.api.calendar.domain.model.ImportReport;

import java.util.List;

public record ImportReportResponse(int eventsCreated, int seriesCreated, int seriesUnfolded, int occurrencesUnfolded,
                                   int duplicates, int ignored, List<Note> notes, boolean written) {

    public record Note(ImportNoteKind kind, String title, String detail) {}

    public static ImportReportResponse from(ImportReport report) {
        return new ImportReportResponse(report.eventsCreated(), report.seriesCreated(), report.seriesUnfolded(),
            report.occurrencesUnfolded(), report.duplicates(), report.ignored(),
            report.notes().stream().map(note -> new Note(note.kind(), note.title(), note.detail())).toList(),
            report.written());
    }
}
