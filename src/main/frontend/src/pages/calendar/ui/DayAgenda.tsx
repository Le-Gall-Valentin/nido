import { useTranslation } from 'react-i18next'
import type { CalendarOccurrence } from '@/entities/calendar'
import { groupByDay } from '../lib/calendarWindow'
import type { ScheduleChange } from '../lib/dragTypes'
import { covers, isBandOccurrence, segmentFor } from '../lib/segments'
import { useDragPreview } from '../model/dragPreview'
import { useOpeningScroll } from '../model/useOpeningScroll'
import { useGridSelection } from '../model/useGridSelection'
import { AllDayBand } from './AllDayBand'
import { HourColumn, HourGutter } from './HourColumn'

interface DayAgendaProps {
  date: string
  occurrences: CalendarOccurrence[]
  onSelectOccurrence: (occurrence: CalendarOccurrence) => void
  /** Off for a viewer: nothing they drag could be saved. */
  canWrite?: boolean
  /** A time picked out in the grid, to add an event over it. Left out for a viewer. */
  onCreateRange?: (range: ScheduleChange) => void
}

/**
 * One day, one column — the same at every width. A single hour column is already the shape a
 * phone wants, so this is the one view that needs no responsive branch.
 */
export function DayAgenda({ date, occurrences, onSelectOccurrence, canWrite = false, onCreateRange }: DayAgendaProps) {
  const { t } = useTranslation('calendar')
  const dayOccurrences = groupByDay(occurrences, [date]).get(date) ?? []
  const timed = dayOccurrences.filter((o) => !isBandOccurrence(o))
  const picking = useGridSelection(onCreateRange)
  const starts = timed.flatMap((o) => { const segment = segmentFor(o, date); return segment?.isStart ? [segment.startMinutes] : [] })
  const gridScroller = useOpeningScroll(date, starts, useDragPreview() !== null)
  const pickedDays = picking.shown?.kind === 'band' ? picking.shown.range : null

  return (
    <div className="rounded-2xl border border-border bg-bg-1">
      <AllDayBand
        day={date}
        canWrite={canWrite}
        picked={pickedDays !== null && covers(pickedDays, date)}
        onPickStart={picking.startBand}
        occurrences={dayOccurrences.filter(isBandOccurrence)}
        onSelectOccurrence={onSelectOccurrence}
      />
      {/* Always rendered, even on a day with no timed event: it is where a dragged event lands on
          a time, and a drag carried across the edge to an empty day would otherwise have only the
          band to land in. Keeping it mounted also keeps its scroll while the next day loads. */}
      <div data-testid="day-grid" className="relative">
        {/* Over the grid, not above it: paging to an empty day mid-drag must not shift the grid. */}
        {dayOccurrences.length === 0 && (
          <p className="pointer-events-none absolute inset-x-0 top-4 z-10 text-center text-sm text-fg-3">{t('empty_day')}</p>
        )}
        <div ref={gridScroller} className="flex items-start max-h-[65vh] overflow-y-auto">
          <HourGutter />
          <div className="flex-1 border-l border-border">
            <HourColumn day={date} occurrences={timed} canWrite={canWrite} onSelectOccurrence={onSelectOccurrence}
            picked={picking.shown?.kind === 'hours' ? picking.shown.range : null} onPickStart={picking.startHours} />
          </div>
        </div>
      </div>
    </div>
  )
}
