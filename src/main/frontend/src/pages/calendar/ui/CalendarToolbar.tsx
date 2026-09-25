import { useTranslation } from 'react-i18next'
import { ChevronLeft, ChevronRight, Plus } from 'lucide-react'
import type { CalendarSourceType } from '@/entities/calendar'
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
  onManageSeries: () => void
  onNewEvent: () => void
}

/** The calendar's title and what sits above the grid: adding, paging, the view, the sources shown. */
export function CalendarToolbar({
  canWrite, view, periodLabel, onShift, onToday, onView, isEnabled, onToggle, onManageSeries, onNewEvent,
}: CalendarToolbarProps) {
  const { t } = useTranslation('calendar')

  return (
    <>
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-fg-0">{t('title')}</h1>
        {canWrite && (
          <div className="flex w-full flex-col gap-2 sm:w-auto sm:flex-row sm:flex-wrap sm:items-center">
            <button type="button" onClick={onManageSeries}
              className="text-center text-sm font-semibold text-accent sm:text-left">
              {t('recurring_series.manage')}
            </button>
            <button type="button" onClick={onNewEvent}
              className="flex w-full items-center justify-center gap-1.5 rounded-[10px] bg-accent px-4 py-2.5 text-sm font-semibold text-white sm:w-auto">
              <Plus className="size-4" /> {t('new_event')}
            </button>
          </div>
        )}
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
                ${view === candidate ? 'bg-accent text-white' : 'text-fg-2 hover:bg-bg-2'}`}
            >
              {t(`view.${candidate}`)}
            </button>
          ))}
        </div>

        <div className="w-full sm:ml-auto sm:w-auto">
          <SourceFilters isEnabled={isEnabled} onToggle={onToggle} />
        </div>
      </div>
    </>
  )
}
