import { useTranslation } from 'react-i18next'
import i18next from 'i18next'
import { useDraggable, useDroppable } from '@dnd-kit/core'
import type { CalendarOccurrence } from '@/entities/calendar'
import { resolveLocale } from '@/shared/lib'
import { groupByDay, monthGridDates } from '../lib/calendarWindow'
import { canReschedule } from '@/features/reschedule-occurrence'
import type { DragData, DropData } from '../lib/dragTypes'
import { covers } from '../lib/segments'
import { fadeClassFor, useDragPreview } from '../model/dragPreview'
import { SOURCE_ORDER, dotClassFor, dotClassForSource } from '../lib/sourceAppearance'
import { formatDay, formatDayAndMonth } from '../lib/periodLabel'

interface MonthGridProps {
  /** ISO date anchoring the month shown. */
  date: string
  occurrences: CalendarOccurrence[]
  /** The household's today, never the browser's. */
  today: string
  onSelectDay: (day: string) => void
  /** A day of the month before or after, pressed: the calendar turns to its month. */
  onSelectMonth: (day: string) => void
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
 *
 * The days of the month before and after are set back — a recessed cell, everything on it faded —
 * and pressing one turns to its month rather than opening it: the six weeks fill the grid, but
 * only one month is being read. Each first of a month is named, so the boundary reads as a date.
 */
export function MonthGrid({
  date, occurrences, today, onSelectDay, onSelectMonth, onSelectOccurrence, canWrite = false,
}: MonthGridProps) {
  const { t } = useTranslation('calendar')
  const locale = resolveLocale(i18next.language)
  const days = monthGridDates(date)
  const byDay = groupByDay(occurrences, days)
  const landing = useDragPreview()?.occurrence
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
          const open = () => (isOutside ? onSelectMonth(day) : onSelectDay(day))
          const faded = isOutside ? 'opacity-50' : ''

          return (
            <DayCell key={day} day={day} canWrite={canWrite} outside={isOutside}>
              {/* A circle around a number, a pill around "1 oct.": the width follows the text. */}
              <button
                type="button"
                onClick={open}
                data-today={isToday || undefined}
                aria-label={t(isOutside ? 'show_month' : 'open_day', { date: formatDay(day, locale) })}
                className={`mb-1 grid h-6 min-w-6 place-items-center rounded-full px-1 text-xs font-semibold
                  after:absolute after:inset-0 after:content-['']
                  ${isToday ? 'bg-accent text-white' : isOutside ? 'text-fg-3' : 'text-fg-1'}`}
              >
                {day.endsWith('-01') ? (
                  <>
                    <span className="md:hidden">1</span>
                    <span className="hidden md:inline">{formatDayAndMonth(day, locale)}</span>
                  </>
                ) : Number(day.slice(8, 10))}
              </button>

              {/* Phone: one dot per source present — never one per occurrence. Faded, the row
                  would be drawn over the stretched day button and swallow the taps meant for it. */}
              <div className={`pointer-events-none flex flex-wrap gap-1 px-0.5 md:hidden ${faded}`}>
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
                {/* A dragged item landing here goes first, so a busy day never hides where it lands. */}
                {landing && covers(landing, day) && <LandingChip occurrence={landing} />}
                {dayOccurrences.slice(0, MAX_LABELS).map((occurrence) => (
                  <OccurrenceChip
                    key={`${occurrence.sourceId}-${day}`}
                    occurrence={occurrence}
                    day={day}
                    canWrite={canWrite}
                    outside={isOutside}
                    onSelect={() => onSelectOccurrence(occurrence)}
                  />
                ))}
                {dayOccurrences.length > MAX_LABELS && (
                  <button
                    type="button"
                    onClick={(event) => { event.stopPropagation(); open() }}
                    className={`relative z-10 px-1 text-left text-[11px] font-semibold text-fg-3 hover:text-fg-1 ${faded}`}
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

/**
 * A day is a drop target only while dragging is possible at all. One outside the month shown takes
 * the page's own ground, darker than the grid's in both themes — unless something is dragged over it.
 */
function DayCell({ day, canWrite, outside, children }: {
  day: string; canWrite: boolean; outside: boolean; children: React.ReactNode
}) {
  const { setNodeRef, isOver } = useDroppable({
    id: `day:${day}`, disabled: !canWrite, data: { target: { kind: 'day', day } } satisfies DropData,
  })
  return (
    <div ref={canWrite ? setNodeRef : undefined}
      className={`relative min-h-[68px] border-b border-r border-border p-1 md:min-h-[116px] md:p-1.5
        ${isOver ? 'bg-accent-dim' : outside ? 'bg-bg-0' : ''}`}>
      {children}
    </div>
  )
}

interface ChipProps {
  occurrence: CalendarOccurrence
  /** The cell this chip sits in: a multi-day event has one chip per day, each its own handle. */
  day: string
  canWrite: boolean
  /** On a day of the month before or after: faded, like the rest of that day. */
  outside: boolean
  onSelect: () => void
}

/**
 * One occurrence inside a month cell. Draggable only when it has a row whose date the calendar may
 * rewrite — see canReschedule. Everything else keeps a normal cursor, so the affordance never
 * promises something the drop would refuse.
 */
function OccurrenceChip({ occurrence, day, canWrite, outside, onSelect }: ChipProps) {
  const draggable = canWrite && canReschedule(occurrence)
  // One opacity at a time: two opacity classes on one element leave the stylesheet to pick.
  const fade = fadeClassFor(useDragPreview(), occurrence) || (outside ? 'opacity-50' : '')
  // Keyed by day too: dnd-kit needs one id per handle, and a trip shows a chip on every day.
  const { attributes, listeners, setNodeRef } = useDraggable({
    id: `cell:${occurrence.sourceId}:${day}`,
    disabled: !draggable,
    data: { intent: { kind: 'move', occurrence, from: 'cell', day } } satisfies DragData,
  })

  return (
    <button
      ref={draggable ? setNodeRef : undefined}
      {...(draggable ? listeners : undefined)}
      {...(draggable ? attributes : undefined)}
      type="button"
      data-draggable={draggable || undefined}
      onClick={(event) => {
        // The cell behind this chip opens the day. Without this the click would do both.
        event.stopPropagation()
        onSelect()
      }}
      className={`relative z-10 flex items-center gap-1 truncate rounded px-1 py-0.5 text-left text-[11px] text-fg-1 hover:bg-bg-2
        ${draggable ? 'touch-manipulation cursor-grab active:cursor-grabbing' : ''} ${fade}`}
    >
      <span className={`size-1.5 shrink-0 rounded-full ${dotClassFor(occurrence)}`} />
      {!occurrence.allDay && occurrence.startTime && (
        <span className="shrink-0 tabular-nums text-fg-3">{occurrence.startTime.slice(0, 5)}</span>
      )}
      <span className="truncate">{occurrence.title}</span>
    </button>
  )
}

/** A dragged item drawn in the cell it would land in. Inert, like every landing preview. */
function LandingChip({ occurrence }: { occurrence: CalendarOccurrence }) {
  return (
    <div data-testid="drag-preview"
      className="pointer-events-none relative z-10 flex items-center gap-1 truncate rounded bg-bg-1 px-1 py-0.5 text-[11px] text-fg-0 shadow-md ring-2 ring-accent">
      <span className={`size-1.5 shrink-0 rounded-full ${dotClassFor(occurrence)}`} />
      {!occurrence.allDay && occurrence.startTime && (
        <span className="shrink-0 tabular-nums text-fg-3">{occurrence.startTime.slice(0, 5)}</span>
      )}
      <span className="truncate">{occurrence.title}</span>
    </div>
  )
}
