import { useTranslation } from 'react-i18next'
import type { CalendarOccurrence } from '@/entities/calendar'
import { groupByDay } from '../lib/calendarWindow'
import { isBandOccurrence } from '../lib/segments'
import { AllDayBand } from './AllDayBand'
import { HourColumn, HourGutter } from './HourColumn'

interface DayAgendaProps {
  date: string
  occurrences: CalendarOccurrence[]
  onSelectOccurrence: (occurrence: CalendarOccurrence) => void
  /** Off for a viewer: nothing they drag could be saved. */
  canWrite?: boolean
}

/**
 * One day, one column — the same at every width. A single hour column is already the shape a
 * phone wants, so this is the one view that needs no responsive branch.
 */
export function DayAgenda({ date, occurrences, onSelectOccurrence, canWrite = false }: DayAgendaProps) {
  const { t } = useTranslation('calendar')
  const dayOccurrences = groupByDay(occurrences, [date]).get(date) ?? []
  const timed = dayOccurrences.filter((o) => !isBandOccurrence(o))

  return (
    <div className="rounded-2xl border border-border bg-bg-1">
      <AllDayBand
        day={date}
        canWrite={canWrite}
        occurrences={dayOccurrences.filter(isBandOccurrence)}
        onSelectOccurrence={onSelectOccurrence}
      />
      {dayOccurrences.length === 0 && (
        <p className="px-3 py-6 text-center text-sm text-fg-3">{t('empty_day')}</p>
      )}
      {timed.length > 0 && (
        <div className="flex max-h-[65vh] overflow-y-auto">
          <HourGutter />
          <div className="flex-1 border-l border-border">
            <HourColumn day={date} occurrences={timed} canWrite={canWrite} onSelectOccurrence={onSelectOccurrence} />
          </div>
        </div>
      )}
    </div>
  )
}
