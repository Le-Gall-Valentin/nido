import { useRef, useState, type KeyboardEvent } from 'react'
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
  const tabRefs = useRef<Partial<Record<Tab, HTMLButtonElement | null>>>({})

  // The ARIA tab pattern: one tab stop, the arrows move between the tabs that are shown.
  const onTabKeyDown = (event: KeyboardEvent) => {
    const at = tabs.indexOf(tab)
    const target = event.key === 'ArrowRight' ? (at + 1) % tabs.length
      : event.key === 'ArrowLeft' ? (at - 1 + tabs.length) % tabs.length
      : event.key === 'Home' ? 0
      : event.key === 'End' ? tabs.length - 1
      : -1
    const next = tabs[target]
    if (!next) return
    event.preventDefault()
    setTab(next)
    tabRefs.current[next]?.focus()
  }

  return (
    <Dialog open onClose={onClose} title={t('title')} showCloseButton maxWidth="max-w-lg">
      <div role="tablist" aria-label={t('title')} className="-mx-1 mb-4 flex gap-1 overflow-x-auto border-b border-border px-1">
        {tabs.map((id) => (
          <button key={id} ref={(el) => { tabRefs.current[id] = el }} type="button" role="tab"
            tabIndex={tab === id ? 0 : -1} onKeyDown={onTabKeyDown} id={`calendar-sync-tab-${id}`} aria-selected={tab === id}
            aria-controls={`calendar-sync-panel-${id}`} onClick={() => setTab(id)}
            className={`-mb-px whitespace-nowrap border-b-2 px-3 py-2 text-sm font-semibold
              ${tab === id ? 'border-accent text-fg-0' : 'border-transparent text-fg-2 hover:text-fg-1'}`}>
            {t(`tabs.${id}`)}
          </button>
        ))}
      </div>
      {/* Focusable: each panel opens on a sentence, not a control (WAI-ARIA tabs pattern). */}
      <div role="tabpanel" tabIndex={0} id={`calendar-sync-panel-${tab}`} aria-labelledby={`calendar-sync-tab-${tab}`}>
        {tab === 'export' && <ExportTab spaceId={spaceId} isPersonal={isPersonal} />}
        {tab === 'subscribe' && <SubscriptionsTab spaceId={spaceId} isPersonal={isPersonal} />}
        {tab === 'import' && <ImportTab spaceId={spaceId} />}
        {tab === 'feeds' && <ExternalFeedsTab spaceId={spaceId} canWrite={canWrite} />}
      </div>
    </Dialog>
  )
}
