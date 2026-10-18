import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Dialog } from '@/shared/ui'
import { ExportTab } from './ExportTab'
import { SubscriptionsTab } from './SubscriptionsTab'
import { ImportTab } from './ImportTab'
import { ExternalFeedsTab } from './ExternalFeedsTab'

type Tab = 'export' | 'subscribe' | 'import' | 'feeds'

interface CalendarSyncDialogProps {
  spaceId: string
  /** A viewer exports and subscribes — they see it all already — but imports nothing and follows nothing. */
  canWrite: boolean
  /** A personal space has nobody else: "only what concerns me" would leave nothing out. */
  isPersonal: boolean
  onClose: () => void
}

/** What goes in and out of a space's calendar, one tab each. */
export function CalendarSyncDialog({ spaceId, canWrite, isPersonal, onClose }: CalendarSyncDialogProps) {
  const { t } = useTranslation('calendarSync')
  const tabs: Tab[] = canWrite ? ['export', 'subscribe', 'import', 'feeds'] : ['export', 'subscribe', 'feeds']
  const [tab, setTab] = useState<Tab>('export')

  return (
    <Dialog open onClose={onClose} title={t('title')} showCloseButton maxWidth="max-w-lg">
      <div role="tablist" aria-label={t('title')} className="-mx-1 mb-4 flex gap-1 overflow-x-auto border-b border-border px-1">
        {tabs.map((id) => (
          <button key={id} type="button" role="tab" id={`calendar-sync-tab-${id}`} aria-selected={tab === id}
            aria-controls={`calendar-sync-panel-${id}`} onClick={() => setTab(id)}
            className={`-mb-px whitespace-nowrap border-b-2 px-3 py-2 text-sm font-semibold
              ${tab === id ? 'border-accent text-fg-0' : 'border-transparent text-fg-2 hover:text-fg-1'}`}>
            {t(`tabs.${id}`)}
          </button>
        ))}
      </div>
      <div role="tabpanel" id={`calendar-sync-panel-${tab}`} aria-labelledby={`calendar-sync-tab-${tab}`}>
        {tab === 'export' && <ExportTab spaceId={spaceId} isPersonal={isPersonal} />}
        {tab === 'subscribe' && <SubscriptionsTab spaceId={spaceId} isPersonal={isPersonal} />}
        {tab === 'import' && <ImportTab spaceId={spaceId} />}
        {tab === 'feeds' && <ExternalFeedsTab spaceId={spaceId} canWrite={canWrite} />}
      </div>
    </Dialog>
  )
}
