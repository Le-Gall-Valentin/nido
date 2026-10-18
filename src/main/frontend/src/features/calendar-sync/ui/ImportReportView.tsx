import { useTranslation } from 'react-i18next'
import type { ImportNote, ImportReport } from '@/entities/calendar'
import { ignoredReasonText } from '../lib/syncWording'

/** The fields whose text can be shortened, each worded under `import.text_field`. */
const TEXT_FIELDS = ['title', 'description', 'location']

/** What an import did or would do, and what deserves a look. The totals come from the report, not from its notes. */
export function ImportReportView({ report }: { report: ImportReport }) {
  const { t } = useTranslation('calendarSync')
  const noteText = (note: ImportNote) => {
    const title = note.title ?? t('import.untitled')
    const raw = note.detail ?? ''
    const detail = note.kind === 'IGNORED' ? ignoredReasonText(t, raw)
      : note.kind === 'TRUNCATED_TEXT' && TEXT_FIELDS.includes(raw) ? t(`import.text_field.${raw}`)
        : raw
    return t(`import.note.${note.kind}`, { title, detail })
  }
  return (
    <div className="flex flex-col gap-2">
      <ul className="flex flex-col gap-0.5 text-sm text-fg-1">
        <li>{t('import.summary.events', { count: report.eventsCreated })}</li>
        <li>{t('import.summary.series', { count: report.seriesCreated })}</li>
        {report.occurrencesUnfolded > 0 && <li>{t('import.summary.unfolded', { count: report.occurrencesUnfolded })}</li>}
        {report.duplicates > 0 && <li>{t('import.summary.duplicates', { count: report.duplicates })}</li>}
        {report.ignored > 0 && <li>{t('import.summary.ignored', { count: report.ignored })}</li>}
      </ul>
      {report.notes.length > 0 && (
        <div className="rounded-lg bg-bg-2 px-3 py-2">
          <p className="mb-1 text-xs font-semibold text-fg-1">{t('import.notes_title')}</p>
          <ul className="flex max-h-48 flex-col gap-0.5 overflow-y-auto text-xs text-fg-2">
            {report.notes.map((note, index) => <li key={index}>{noteText(note)}</li>)}
          </ul>
        </div>
      )}
    </div>
  )
}
