import { useTranslation } from 'react-i18next'
import { MAX_IMPORT_NOTES, type ImportNote, type ImportReport } from '@/entities/calendar'
import { TEXT_FIELDS, ignoredReasonText } from '../lib/syncWording'

/** The wording of a note: a zone note with no zone, or a partial unfolding with nothing in the window, say so. */
function noteKey(note: ImportNote, detail: string): string {
  if ((note.kind === 'CONVERTED_ZONE' || note.kind === 'UNKNOWN_ZONE') && !detail) return `${note.kind}_unnamed`
  if (note.kind === 'UNFOLDED_PARTIAL' && Number(detail) === 0) return 'UNFOLDED_PARTIAL_zero'
  return note.kind
}

/** What an import did or would do: its totals, from the report itself rather than from its notes. */
export function ImportTotals({ report }: { report: ImportReport }) {
  const { t } = useTranslation('calendarSync')
  return (
    <ul className="flex flex-col gap-0.5 text-sm text-fg-1">
      <li>{t('import.summary.events', { count: report.eventsCreated })}</li>
      {/* Those occurrences are events of their own: counted among the events, said under them. */}
      {report.occurrencesUnfolded > 0 && (
        <li className="pl-3 text-fg-2">{t('import.summary.unfolded', { count: report.occurrencesUnfolded })}</li>
      )}
      <li>{t('import.summary.series', { count: report.seriesCreated })}</li>
      {report.seriesUnfolded > 0 && <li>{t('import.summary.series_unfolded', { count: report.seriesUnfolded })}</li>}
      {report.duplicates > 0 && <li>{t('import.summary.duplicates', { count: report.duplicates })}</li>}
      {report.ignored > 0 && <li>{t('import.summary.ignored', { count: report.ignored })}</li>}
    </ul>
  )
}

/** What deserves a look: up to {@link MAX_IMPORT_NOTES} notes, read at will rather than announced. */
export function ImportNotes({ report }: { report: ImportReport }) {
  const { t } = useTranslation('calendarSync')
  const noteText = (note: ImportNote) => {
    const title = note.title ?? t('import.untitled')
    const raw = note.detail ?? ''
    const detail = note.kind === 'IGNORED' ? ignoredReasonText(t, raw)
      : note.kind === 'TRUNCATED_TEXT' && TEXT_FIELDS.includes(raw) ? t(`import.text_field.${raw}`)
        : raw
    const key = noteKey(note, raw)
    // A number of events is said with its plural: "1 événement séparé", "3 événements séparés".
    const counted = key === 'UNFOLDED' || key === 'UNFOLDED_PARTIAL'
    return t(`import.note.${key}`, counted ? { title, detail, count: Number(raw) } : { title, detail })
  }
  if (report.notes.length === 0) return null
  return (
    <div className="rounded-lg bg-bg-2 px-3 py-2">
      <p className="mb-1 text-xs font-semibold text-fg-1">{t('import.notes_title')}</p>
      <ul className="flex max-h-48 flex-col gap-0.5 overflow-y-auto text-xs text-fg-2">
        {report.notes.map((note, index) => <li key={index}>{noteText(note)}</li>)}
      </ul>
      {report.notes.length >= MAX_IMPORT_NOTES && (
        <p className="mt-1 text-xs text-fg-3">{t('import.notes_capped', { count: MAX_IMPORT_NOTES })}</p>
      )}
    </div>
  )
}
