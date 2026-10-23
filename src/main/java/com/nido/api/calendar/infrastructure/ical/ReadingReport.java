package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.model.ImportNote;
import com.nido.api.calendar.domain.model.ImportNoteKind;

import java.util.ArrayList;
import java.util.List;

/** What the reading of a file says: the events it set aside, counted, and the notes its report gives. */
final class ReadingReport {

    /** Where a report stands: what reading one group said is taken back to it when that group is given up whole. */
    record Mark(int notes, int ignored) {}

    private final List<ImportNote> notes = new ArrayList<>();
    private int ignored;

    /** One more event (or group of a UID) not imported, and why. */
    void ignore(String title, String reason) {
        ignored++;
        note(ImportNoteKind.IGNORED, title, reason);
    }

    void note(ImportNoteKind kind, String title, String detail) {
        notes.add(new ImportNote(kind, title, detail));
    }

    Mark mark() {
        return new Mark(notes.size(), ignored);
    }

    void rewindTo(Mark mark) {
        notes.subList(mark.notes(), notes.size()).clear();
        ignored = mark.ignored();
    }

    int ignored() {
        return ignored;
    }

    List<ImportNote> notes() {
        return List.copyOf(notes);
    }
}
