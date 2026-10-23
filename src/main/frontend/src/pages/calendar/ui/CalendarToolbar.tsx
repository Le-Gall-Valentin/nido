import { useTranslation } from 'react-i18next'
import { CalendarSync, ChevronLeft, ChevronRight, Plus, Repeat } from 'lucide-react'
import type { CalendarSourceType, ExternalFeed } from '@/entities/calendar'
import { Button, PageActions } from '@/shared/ui'
import type { CalendarView } from '../lib/calendarWindow'
import { SourceFilters } from './SourceFilters'

const VIEWS: CalendarView[] = ['month', 'week', 'day']

interface CalendarToolbarProps {
  canWrite: boolean
  view: CalendarView
  /** Where paging has arrived, named at the granularity the view shows. */
  periodLabel: string
  onShift: (direction: -1 | 1) => void
  onToday: () => void
  onView: (view: CalendarView) => void
  isEnabled: (source: CalendarSourceType) => boolean
  onToggle: (source: CalendarSourceType) => void
  /** The calendars the space follows, each filtered on its own. */
  feeds: ExternalFeed[]
  isFeedEnabled: (feedId: string) => boolean
  onToggleFeed: (feedId: string) => void
  onSync: () => void
  onManageSeries: () => void
  onNewEvent: () => void
}

/** The calendar's title and what sits above the grid: adding, paging, the view, the sources shown. */
export function CalendarToolbar({
  canWrite, view, periodLabel, onShift, onToday, onView, isEnabled, onToggle, feeds, isFeedEnabled, onToggleFeed, onSync,
  onManageSeries, onNewEvent,
}: CalendarToolbarProps) {
  const { t } = useTranslation('calendar')

  return (
    <>
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-fg-0">{t('title')}</h1>
        <PageActions>
          {/* Everyone may export and subscribe: a viewer sees the whole calendar already. */}
          <Button type="button" icon={CalendarSync} onClick={onSync}>{t('sync')}</Button>
          {canWrite && (
            <>
              {/* Its short name, until the page is wide enough for the long one beside the sidebar, is part of its full
                  one, which is what it is called. */}
              <Button type="button" icon={Repeat} onClick={onManageSeries} aria-label={t('recurring_series.open')}>
                <span className="lg:hidden">{t('recurring_series.open_short')}</span>
                <span className="hidden lg:inline">{t('recurring_series.open')}</span>
              </Button>
              <Button type="button" variant="primary" icon={Plus} onClick={onNewEvent}>{t('new_event')}</Button>
            </>
          )}
        </PageActions>
      </div>

      <div className="mb-4 flex flex-wrap items-center gap-2">
        <div className="flex items-center gap-1">
          <button type="button" aria-label={t('previous_period')} onClick={() => onShift(-1)}
            className="grid size-8 place-items-center rounded-lg border border-border text-fg-2 hover:bg-bg-2">
            <ChevronLeft className="size-4" />
          </button>
          <button type="button" aria-label={t('next_period')} onClick={() => onShift(1)}
            className="grid size-8 place-items-center rounded-lg border border-border text-fg-2 hover:bg-bg-2">
            <ChevronRight className="size-4" />
          </button>
          <button type="button" onClick={onToday}
            className="rounded-lg border border-border px-3 py-1.5 text-sm font-semibold text-fg-1 hover:bg-bg-2">
            {t('today')}
          </button>
        </div>

        {/* aria-live, because paging is what changes it and the grid gives a screen reader no
            other way to hear where it landed. */}
        <h2 aria-live="polite" className="order-first w-full text-lg font-semibold text-fg-0 sm:order-none sm:w-auto">
          {periodLabel}
        </h2>

        <div role="group" className="flex w-full rounded-lg border border-border sm:w-auto">
          {VIEWS.map((candidate) => (
            <button
              key={candidate}
              type="button"
              aria-pressed={view === candidate}
              onClick={() => onView(candidate)}
              className={`flex-1 px-3 py-1.5 text-sm font-semibold first:rounded-l-lg last:rounded-r-lg sm:flex-none
                ${view === candidate ? 'bg-accent text-on-accent' : 'text-fg-2 hover:bg-bg-2'}`}
            >
              {t(`view.${candidate}`)}
            </button>
          ))}
        </div>

        <div className="w-full sm:ml-auto sm:w-auto">
          <SourceFilters isEnabled={isEnabled} onToggle={onToggle} feeds={feeds} isFeedEnabled={isFeedEnabled}
            onToggleFeed={onToggleFeed} />
        </div>
      </div>
    </>
  )
}
