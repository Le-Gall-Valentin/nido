import type { DragEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, CTA_BUTTON_STYLE, Spinner } from '@/shared/ui'
import { useAnalyseImport, useImportFile } from '@/entities/calendar'
import { ImportReportView } from './ImportReportView'
import { syncErrorMessage } from '../lib/syncWording'

/**
 * Choose a file, read what importing it would do, then import it — the server reads it again then: nothing is kept
 * between the two. The file imported is the one the analysis on screen was made from.
 */
export function ImportTab({ spaceId }: { spaceId: string }) {
  const { t } = useTranslation('calendarSync')
  const analyse = useAnalyseImport(spaceId)
  const importFile = useImportFile(spaceId)

  const choose = (picked: File | undefined) => {
    if (!picked) return
    importFile.reset()
    analyse.mutate(picked)
  }
  const onDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault()
    choose(e.dataTransfer.files[0])
  }
  const startOver = () => {
    analyse.reset()
    importFile.reset()
  }

  const analysed = analyse.data
  const report = importFile.data ?? analysed
  const toImport = analysed ? analysed.eventsCreated + analysed.seriesCreated : 0
  const error = importFile.error ?? analyse.error

  return (
    <div className="flex flex-col gap-3">
      <p className="text-sm text-fg-2">{t('import.intro')}</p>
      {!report && !analyse.isPending && (
        <div onDragOver={(e) => e.preventDefault()} onDrop={onDrop}
          className="flex flex-col items-center gap-1 rounded-lg border-2 border-dashed border-border px-4 py-6 text-center">
          <label className="cursor-pointer text-sm font-semibold text-accent">
            {t('import.choose')}
            <input type="file" accept=".ics,text/calendar" aria-label={t('import.choose')} className="sr-only"
              onChange={(e) => { choose(e.target.files?.[0]); e.target.value = '' }} />
          </label>
          <span className="text-xs text-fg-3">{t('import.drop')}</span>
        </div>
      )}
      {(analyse.isPending || importFile.isPending) && (
        <div className="flex items-center gap-2 text-sm text-fg-2">
          <Spinner fullscreen={false} label={t(importFile.isPending ? 'import.importing' : 'import.analysing')} />
          {t(importFile.isPending ? 'import.importing' : 'import.analysing')}
        </div>
      )}
      {error && <p role="alert" className="text-sm text-status-red">{syncErrorMessage(t, error)}</p>}
      {report && <ImportReportView report={report} />}
      {report && !report.written && toImport === 0 && <p className="text-sm text-fg-2">{t('import.nothing')}</p>}
      {report?.written && <p className="text-sm font-semibold text-status-green">{t('import.done')}</p>}
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
