import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { useDraggable, useDroppable } from '@dnd-kit/core'
import type { CalendarOccurrence } from '@/entities/calendar'
import { groupByDay, weekDates } from '../lib/calendarWindow'
import type { DragData, DropData } from '../lib/dragTypes'
import { isDraggable } from '../lib/isDraggable'
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
            <PhoneDaySection key={day} day={day} canWrite={canWrite}>
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
                      <PhoneRow occurrence={occurrence} day={day} canWrite={canWrite}
                        label={occurrence.allDay ? t('all_day_short') : occurrence.startTime?.slice(0, 5) ?? ''}
                        onSelect={() => onSelectOccurrence(occurrence)} />
                    </li>
                  ))}
                </ul>
              )}
            </PhoneDaySection>
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

/** A phone day section is a drop target for the whole day: a row moves day to day, keeping its time. */
function PhoneDaySection({ day, canWrite, children }: { day: string; canWrite: boolean; children: ReactNode }) {
  const { setNodeRef, isOver } = useDroppable({
    id: `day:${day}`, disabled: !canWrite, data: { target: { kind: 'day', day } } satisfies DropData,
  })
  return (
    <section ref={setNodeRef} data-testid="week-day-section"
      className={`rounded-2xl border border-border bg-bg-1 p-3 ${isOver ? 'ring-2 ring-accent' : ''}`}>
      {children}
    </section>
  )
}

function PhoneRow({ occurrence, day, canWrite, label, onSelect }: {
  occurrence: CalendarOccurrence; day: string; canWrite: boolean; label: string; onSelect: () => void
}) {
  const draggable = canWrite && isDraggable(occurrence)
  const { setNodeRef, listeners, attributes, isDragging } = useDraggable({
    id: `row:${occurrence.sourceId}:${day}`, disabled: !draggable,
    data: { intent: { kind: 'move', occurrence, from: 'row', day } } satisfies DragData,
  })
  return (
    <button ref={draggable ? setNodeRef : undefined} {...(draggable ? listeners : {})} {...(draggable ? attributes : {})}
      type="button" data-draggable={draggable || undefined} data-drag-origin="row" onClick={onSelect}
      // touch-manipulation, never touch-none: the list must keep scrolling under a finger that
      // touches a row. TouchSensor blocks the scroll itself, once the long press has started a drag.
      className={`flex w-full touch-manipulation items-center gap-2 rounded px-2 py-1.5 text-left text-xs font-medium ${tintClassFor(occurrence)}
        ${isDragging ? 'opacity-40' : ''}`}>
      <span className="shrink-0 tabular-nums">{label}</span>
      <span className="truncate">{occurrence.title}</span>
    </button>
  )
}
