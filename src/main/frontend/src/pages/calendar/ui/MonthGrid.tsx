import { useTranslation } from 'react-i18next'
import type { CalendarOccurrence } from '@/entities/calendar'
import { groupByDay, monthGridDates } from '../lib/calendarWindow'
import { SOURCE_ORDER, dotClassFor, dotClassForSource } from '../lib/sourceAppearance'

interface MonthGridProps {
  /** ISO date anchoring the month shown. */
  date: string
  occurrences: CalendarOccurrence[]
  /** The household's today, never the browser's. */
  today: string
  onSelectDay: (day: string) => void
  onSelectOccurrence: (occurrence: CalendarOccurrence) => void
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
 * On a phone a cell is about 45px wide — too narrow for any label — so it shows one dot per
 * source present, never one per occurrence. A busy Saturday is then still legible at a glance,
 * and the dots line up column to column because their order is fixed.
 */
export function MonthGrid({ date, occurrences, today, onSelectDay, onSelectOccurrence }: MonthGridProps) {
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
            <div key={day} className="min-h-[68px] border-b border-r border-border p-1 md:min-h-[116px] md:p-1.5">
              <button
                type="button"
                onClick={() => onSelectDay(day)}
                data-today={isToday || undefined}
                aria-label={t('open_day', { date: day })}
                className={`mb-1 grid size-6 place-items-center rounded-full text-xs font-semibold
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
                  <button
                    key={`${occurrence.sourceId}-${day}`}
                    type="button"
                    onClick={(event) => {
                      // The cell behind this chip opens the day. Without this the click would do both.
                      event.stopPropagation()
                      onSelectOccurrence(occurrence)
                    }}
                    className="flex items-center gap-1 truncate rounded px-1 py-0.5 text-left text-[11px] text-fg-1 hover:bg-bg-2"
                  >
                    <span className={`size-1.5 shrink-0 rounded-full ${dotClassFor(occurrence)}`} />
                    {!occurrence.allDay && occurrence.startTime && (
                      <span className="shrink-0 tabular-nums text-fg-3">{occurrence.startTime.slice(0, 5)}</span>
                    )}
                    <span className="truncate">{occurrence.title}</span>
                  </button>
                ))}
                {dayOccurrences.length > MAX_LABELS && (
                  <button
                    type="button"
                    onClick={(event) => { event.stopPropagation(); onSelectDay(day) }}
                    className="px-1 text-left text-[11px] font-semibold text-fg-3 hover:text-fg-1"
                  >
                    +{dayOccurrences.length - MAX_LABELS}
                  </button>
                )}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
