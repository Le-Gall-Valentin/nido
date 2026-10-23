import { useEffect, useRef, useState, type DragEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, CTA_BUTTON_STYLE } from '@/shared/ui'
import { MAX_IMPORT_BYTES, useAnalyseImport, useImportFile } from '@/entities/calendar'
import { ImportNotes, ImportTotals } from './ImportReport'
import { syncErrorMessage } from '../lib/syncWording'

/**
 * Choose a file, read what importing it would do, then import it — the server reads it again then: nothing is kept
 * between the two. The file imported is the one the analysis on screen was made from.
 */
export function ImportTab({ spaceId }: { spaceId: string }) {
  const { t } = useTranslation('calendarSync')
  const analyse = useAnalyseImport(spaceId)
  const importFile = useImportFile(spaceId)
  // A file the server would refuse for its size is refused here, before it is sent: over a slow line, sending 50 MB
  // to be told so takes long, and a server that closes the connection mid-upload reads as a network failure.
  const [tooLarge, setTooLarge] = useState(false)

  const region = useRef<HTMLDivElement>(null)
  const restoreFocus = useRef(false)
  const busy = analyse.isPending || importFile.isPending

  // After "import another file" the new picker takes the focus the old one lost.
  const pickerRef = (input: HTMLInputElement | null) => {
    if (input && restoreFocus.current) {
      restoreFocus.current = false
      input.focus()
    }
  }

  const choose = (picked: File | undefined) => {
    if (!picked) return
    importFile.reset()
    analyse.reset()
    setTooLarge(picked.size > MAX_IMPORT_BYTES)
    if (picked.size <= MAX_IMPORT_BYTES) analyse.mutate(picked)
  }
  const onDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault()
    choose(e.dataTransfer.files[0])
  }
  const startOver = () => {
    restoreFocus.current = true
    setTooLarge(false)
    analyse.reset()
    importFile.reset()
  }

  const analysed = analyse.data
  const report = importFile.data ?? analysed
  const toImport = analysed ? analysed.eventsCreated + analysed.seriesCreated : 0
  const error = tooLarge ? t('errors.file_too_large')
    : (importFile.error ?? analyse.error) ? syncErrorMessage(t, importFile.error ?? analyse.error) : null

  // The picker unmounts as the work starts: focus goes to the region that says what is going on, and stays there when
  // the report arrives (the pending render can be skipped when the answer is quick).
  useEffect(() => {
    if (busy || report) region.current?.focus()
  }, [busy, report])

  return (
    <div className="flex flex-col gap-3">
      <p className="text-sm text-fg-2">{t('import.intro')}</p>
      {!report && !analyse.isPending && (
        <div onDragOver={(e) => e.preventDefault()} onDrop={onDrop} data-testid="import-drop"
          className="flex flex-col items-center gap-1 rounded-lg border-2 border-dashed border-border px-4 py-6 text-center">
          <label className="cursor-pointer rounded-md px-2 py-1 text-sm font-semibold text-accent focus-within:shadow-[0_0_0_3px_var(--color-accent-ring)]">
            {t('import.choose')}
            <input type="file" accept=".ics,text/calendar" aria-label={t('import.choose')} className="sr-only" ref={pickerRef}
              onChange={(e) => { choose(e.target.files?.[0]); e.target.value = '' }} />
          </label>
          <span className="text-xs text-fg-3">{t('import.drop')}</span>
        </div>
      )}
      {error && <p role="alert" className="text-sm text-status-red">{error}</p>}
      {/* What is announced: the work under way, the totals and the outcome — never the notes, up to 200 of them. */}
      <div ref={region} role="status" tabIndex={-1} aria-busy={busy} className="flex flex-col gap-3 outline-none">
        {busy && (
          <p className="text-sm text-fg-2 motion-safe:animate-pulse">
            {t(importFile.isPending ? 'import.importing' : 'import.analysing')}
          </p>
        )}
        {report && <ImportTotals report={report} />}
        {report && !report.written && toImport === 0 && <p className="text-sm text-fg-2">{t('import.nothing')}</p>}
        {report?.written && <p className="text-sm font-semibold text-status-green">{t('import.done')}</p>}
      </div>
      {report && <ImportNotes report={report} />}
      <div className="flex justify-end gap-2">
        {(report || error) && !importFile.isPending && <Button type="button" onClick={startOver}>{t('import.again')}</Button>}
        {report && !report.written && toImport > 0 && analyse.variables && (
          <Button type="button" style={CTA_BUTTON_STYLE} disabled={importFile.isPending}
            onClick={() => importFile.mutate(analyse.variables)}>
            {t('import.confirm', { count: toImport })}
          </Button>
        )}
      </div>
    </div>
  )
}
