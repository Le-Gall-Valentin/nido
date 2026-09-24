import { useTranslation } from 'react-i18next'
import { useDraggable, useDroppable } from '@dnd-kit/core'
import type { CalendarOccurrence } from '@/entities/calendar'
import { groupByDay, monthGridDates } from '../lib/calendarWindow'
import { isDraggable } from '../lib/isDraggable'
import type { DragData, DropData } from '../lib/dragTypes'
import { SOURCE_ORDER, dotClassFor, dotClassForSource } from '../lib/sourceAppearance'

interface MonthGridProps {
  /** ISO date anchoring the month shown. */
  date: string
  occurrences: CalendarOccurrence[]
  /** The household's today, never the browser's. */
  today: string
  onSelectDay: (day: string) => void
  onSelectOccurrence: (occurrence: CalendarOccurrence) => void
  /** Off for a viewer: nothing they drag could be saved. */
  canWrite?: boolean
}

const WEEKDAY_KEYS = ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'] as const

/** How many labels fit in a desktop cell before the rest collapse into a "+N". */
const MAX_LABELS = 3

/**
 * The month view, in two layouts that share one DOM.
 *
 * Both are rendered and switched with Tailwind's `md:` rather than a matchMedia branch: a media
 * query read in JavaScript is wrong on the first paint and untestable without stubbing, whereas
 * this is correct before hydration and lets a test assert on both layouts at once.
 *
 * The whole cell opens its day, not just the number in its corner: the day button's ::after is
 * stretched over the cell, and the chips sit above it so they keep their own click. That pattern
 * keeps one real button per day — a click handler on the cell div would have no keyboard path,
 * and a cell that was itself a button could not contain the chips' buttons.
 *
 * On a phone a cell is about 45px wide — too narrow for any label — so it shows one dot per
 * source present, never one per occurrence. A busy Saturday is then still legible at a glance,
 * and the dots line up column to column because their order is fixed.
 */
export function MonthGrid({
  date, occurrences, today, onSelectDay, onSelectOccurrence, canWrite = false,
}: MonthGridProps) {
  const { t } = useTranslation('calendar')
  const days = monthGridDates(date)
  const byDay = groupByDay(occurrences, days)
  const shownMonth = date.slice(0, 7)

  return (
    <div>
      <div className="grid grid-cols-7 border-b border-border pb-1.5">
        {WEEKDAY_KEYS.map((key) => (
          <div key={key} className="text-center text-[11px] font-semibold uppercase tracking-wide text-fg-3">
            {t(`weekday_short.${key}`)}
          </div>
        ))}
      </div>

      <div className="grid grid-cols-7">
        {days.map((day) => {
          const dayOccurrences = byDay.get(day) ?? []
          const isToday = day === today
          const isOutside = day.slice(0, 7) !== shownMonth
          const sourcesPresent = SOURCE_ORDER.filter(
            (source) => dayOccurrences.some((o) => o.source === source))

          return (
            <DayCell key={day} day={day} canWrite={canWrite}>
              <button
                type="button"
                onClick={() => onSelectDay(day)}
                data-today={isToday || undefined}
                aria-label={t('open_day', { date: day })}
                className={`mb-1 grid size-6 place-items-center rounded-full text-xs font-semibold
                  after:absolute after:inset-0 after:content-['']
                  ${isToday ? 'bg-accent text-white' : isOutside ? 'text-fg-4' : 'text-fg-1'}`}
              >
                {Number(day.slice(8, 10))}
              </button>

              {/* Phone: one dot per source present — never one per occurrence. */}
              <div className="flex flex-wrap gap-1 px-0.5 md:hidden">
                {sourcesPresent.map((source) => (
                  <span
                    key={source}
                    data-testid="day-dot"
                    data-source={source}
                    className={`size-1.5 rounded-full ${dotClassForSource(source)}`}
                  />
                ))}
              </div>

              {/* Desktop: the labels themselves, capped so a busy day cannot overflow its cell. */}
              <div className="hidden flex-col gap-0.5 md:flex">
                {dayOccurrences.slice(0, MAX_LABELS).map((occurrence) => (
                  <OccurrenceChip
                    key={`${occurrence.sourceId}-${day}`}
                    occurrence={occurrence}
                    canWrite={canWrite}
                    onSelect={() => onSelectOccurrence(occurrence)}
                  />
                ))}
                {dayOccurrences.length > MAX_LABELS && (
                  <button
                    type="button"
                    onClick={(event) => { event.stopPropagation(); onSelectDay(day) }}
                    className="relative z-10 px-1 text-left text-[11px] font-semibold text-fg-3 hover:text-fg-1"
                  >
                    +{dayOccurrences.length - MAX_LABELS}
                  </button>
                )}
              </div>
            </DayCell>
          )
        })}
      </div>
    </div>
  )
}

/** A day is a drop target only while dragging is possible at all. */
function DayCell({ day, canWrite, children }: { day: string; canWrite: boolean; children: React.ReactNode }) {
  const { setNodeRef, isOver } = useDroppable({
    id: `day:${day}`, disabled: !canWrite, data: { target: { kind: 'day', day } } satisfies DropData,
  })
  return (
    <div ref={canWrite ? setNodeRef : undefined}
      className={`relative min-h-[68px] border-b border-r border-border p-1 md:min-h-[116px] md:p-1.5
        ${isOver ? 'bg-accent-dim' : ''}`}>
      {children}
    </div>
  )
}

interface ChipProps {
  occurrence: CalendarOccurrence
  canWrite: boolean
  onSelect: () => void
}

/**
 * One occurrence inside a month cell. Draggable only when it has a row whose date the calendar may
 * rewrite — see isDraggable. Everything else keeps a normal cursor, so the affordance never
 * promises something the drop would refuse.
 */
function OccurrenceChip({ occurrence, canWrite, onSelect }: ChipProps) {
  const draggable = canWrite && isDraggable(occurrence)
  const { attributes, listeners, setNodeRef, transform } = useDraggable({
    id: `cell:${occurrence.sourceId}`,
    disabled: !draggable,
    data: { intent: { kind: 'move', occurrence, from: 'cell' } } satisfies DragData,
  })

  return (
    <button
      ref={draggable ? setNodeRef : undefined}
      {...(draggable ? listeners : undefined)}
      {...(draggable ? attributes : undefined)}
      type="button"
      data-draggable={draggable || undefined}
      style={transform ? { transform: `translate3d(${transform.x}px, ${transform.y}px, 0)` } : undefined}
      onClick={(event) => {
        // The cell behind this chip opens the day. Without this the click would do both.
        event.stopPropagation()
        onSelect()
      }}
      className={`relative z-10 flex items-center gap-1 truncate rounded px-1 py-0.5 text-left text-[11px] text-fg-1 hover:bg-bg-2
        ${draggable ? 'touch-none cursor-grab active:cursor-grabbing' : ''}`}
    >
      <span className={`size-1.5 shrink-0 rounded-full ${dotClassFor(occurrence)}`} />
      {!occurrence.allDay && occurrence.startTime && (
        <span className="shrink-0 tabular-nums text-fg-3">{occurrence.startTime.slice(0, 5)}</span>
      )}
      <span className="truncate">{occurrence.title}</span>
    </button>
  )
}
