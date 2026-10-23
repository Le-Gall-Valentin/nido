import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, CTA_BUTTON_STYLE } from '@/shared/ui'
import { saveBlob } from '@/shared/lib'
import { useExportCalendar, type ExportableSource } from '@/entities/calendar'
import { OnlyMineToggle, SourceChecklist } from './SourceChecklist'
import { syncErrorMessage } from '../lib/syncWording'

export function ExportTab({ spaceId, isPersonal }: { spaceId: string; isPersonal: boolean }) {
  const { t } = useTranslation('calendarSync')
  const [sources, setSources] = useState<ExportableSource[]>(['EVENT'])
  const [onlyMine, setOnlyMine] = useState(false)
  const exportCalendar = useExportCalendar(spaceId)

  const download = () => exportCalendar.mutate({ sources, onlyMine },
    { onSuccess: ({ blob, filename }) => saveBlob(blob, filename) })
  // A failure is about the choice it was made for: changing the choice takes it away.
  const changeSources = (next: ExportableSource[]) => {
    exportCalendar.reset()
    setSources(next)
  }
  const changeOnlyMine = (next: boolean) => {
    exportCalendar.reset()
    setOnlyMine(next)
  }

  return (
    <div className="flex flex-col gap-3">
      <p className="text-sm text-fg-2">{t('export.intro')}</p>
      <SourceChecklist selected={sources} onChange={changeSources} />
      {!isPersonal && <OnlyMineToggle checked={onlyMine} onChange={changeOnlyMine} />}
      {exportCalendar.isError && <p role="alert" className="text-sm text-status-red">{syncErrorMessage(t, exportCalendar.error)}</p>}
      <p role="status" className="min-h-5 text-sm text-fg-2">{exportCalendar.isPending ? t('export.preparing') : ''}</p>
      <div className="flex justify-end">
        <Button type="button" style={CTA_BUTTON_STYLE} disabled={sources.length === 0 || exportCalendar.isPending}
          onClick={download}>
          {t('export.download')}
        </Button>
      </div>
    </div>
  )
}
