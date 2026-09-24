import { useTranslation } from 'react-i18next'
import type { CalendarOccurrence } from '@/entities/calendar'
import { groupByDay, weekDates } from '../lib/calendarWindow'
import { isBandOccurrence } from '../lib/segments'
import { tintClassFor } from '../lib/sourceAppearance'
import { AllDayBand } from './AllDayBand'
import { HourColumn, HourGutter } from './HourColumn'

interface WeekGridProps {
  date: string
  occurrences: CalendarOccurrence[]
  today: string
  onSelectDay: (day: string) => void
  onSelectOccurrence: (occurrence: CalendarOccurrence) => void
  /** Off for a viewer: nothing they drag could be saved. */
  canWrite?: boolean
}

function dayLabel(day: string): string {
  return String(Number(day.slice(8, 10)))
}

/**
 * The week, in two genuinely different shapes.
 *
 * On a desktop it is a real hour grid: seven columns, blocks positioned by time. On a phone that
 * same grid gives each day about 40px — unreadable and untappable — so the phone layout is a
 * stack of seven day sections instead. This is the one place where the two layouts are not the
 * same thing at different sizes, and it is deliberate: a thumb reads a list far better than a
 * forty-pixel column.
 */
export function WeekGrid({ date, occurrences, today, onSelectDay, onSelectOccurrence, canWrite = false }: WeekGridProps) {
  const { t } = useTranslation('calendar')
  const days = weekDates(date)
  const byDay = groupByDay(occurrences, days)

  return (
    <>
      {/* Phone: seven stacked day sections, no hour grid at all. */}
      <div className="flex flex-col gap-3 md:hidden">
        {days.map((day) => {
          const dayOccurrences = byDay.get(day) ?? []
          return (
            <section key={day} data-testid="week-day-section" className="rounded-2xl border border-border bg-bg-1 p-3">
              <button
                type="button"
                onClick={() => onSelectDay(day)}
                className="mb-2 flex items-center gap-2 text-sm font-semibold text-fg-0"
              >
                <span className={`grid size-6 place-items-center rounded-full text-xs
                  ${day === today ? 'bg-accent text-white' : 'bg-bg-3 text-fg-1'}`}>
                  {dayLabel(day)}
                </span>
                {t(`weekday_short.${['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'][days.indexOf(day)]}`)}
              </button>
              {dayOccurrences.length === 0 ? (
                <p className="text-xs text-fg-3">{t('empty_day')}</p>
              ) : (
                <ul className="flex flex-col gap-1">
                  {dayOccurrences.map((occurrence) => (
                    <li key={`${occurrence.sourceId}-${day}`}>
                      <button
                        type="button"
                        onClick={() => onSelectOccurrence(occurrence)}
                        className={`flex w-full items-center gap-2 rounded px-2 py-1.5 text-left text-xs font-medium ${tintClassFor(occurrence)}`}
                      >
                        <span className="shrink-0 tabular-nums">
                          {occurrence.allDay ? t('all_day_short') : occurrence.startTime?.slice(0, 5)}
                        </span>
                        <span className="truncate">{occurrence.title}</span>
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          )
        })}
      </div>

      {/* Desktop: the real hour grid. */}
      <div className="hidden md:block">
        <div className="flex">
          <div className="w-10 shrink-0" />
          {days.map((day) => (
            <button
              key={day}
              type="button"
              onClick={() => onSelectDay(day)}
              className="flex flex-1 flex-col items-center gap-0.5 border-b border-border py-1.5"
            >
              <span className="text-[11px] font-semibold uppercase tracking-wide text-fg-3">
                {t(`weekday_short.${['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'][days.indexOf(day)]}`)}
              </span>
              <span className={`grid size-6 place-items-center rounded-full text-xs font-semibold
                ${day === today ? 'bg-accent text-white' : 'text-fg-1'}`}>
                {dayLabel(day)}
              </span>
            </button>
          ))}
        </div>

        <div className="flex">
          <div className="w-10 shrink-0" />
          {/* One band per day: each is its own drop target, so a timed event dropped on it becomes
              all-day on that day. */}
          <div className="grid flex-1 grid-cols-7">
            {days.map((day) => (
              <div key={day} className="min-w-0 border-l border-border">
                <AllDayBand day={day} occurrences={(byDay.get(day) ?? []).filter(isBandOccurrence)}
                  canWrite={canWrite} onSelectOccurrence={onSelectOccurrence} />
              </div>
            ))}
          </div>
        </div>

        <div className="flex max-h-[60vh] overflow-y-auto">
          <HourGutter />
          {days.map((day) => (
            <div key={day} className="flex-1 border-l border-border">
              <HourColumn
                day={day}
                canWrite={canWrite}
                occurrences={(byDay.get(day) ?? []).filter((o) => !isBandOccurrence(o))}
                onSelectOccurrence={onSelectOccurrence}
              />
            </div>
          ))}
        </div>
      </div>
    </>
  )
}
