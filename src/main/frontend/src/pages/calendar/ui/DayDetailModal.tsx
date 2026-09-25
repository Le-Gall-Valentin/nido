import { useTranslation } from 'react-i18next'
import i18next from 'i18next'
import { MapPin, Plus } from 'lucide-react'
import { Dialog, Button, CTA_BUTTON_STYLE } from '@/shared/ui'
import { resolveLocale } from '@/shared/lib'
import type { CalendarOccurrence } from '@/entities/calendar'
import { groupByDay } from '../lib/calendarWindow'
import { formatPeriodLabel, formatSpan } from '../lib/periodLabel'
import { SOURCE_ORDER, dotClassFor } from '../lib/sourceAppearance'

interface DayDetailModalProps {
  date: string
  occurrences: CalendarOccurrence[]
  /** Off for a viewer: the add button would lead to a form whose save is refused. */
  canWrite: boolean
  onCreateEvent: (date: string) => void
  onSelectOccurrence: (occurrence: CalendarOccurrence) => void
  onClose: () => void
}

/** "18:00 – 19:30", from times the API may send with seconds. */
function timeSpan(occurrence: CalendarOccurrence): string | null {
  if (occurrence.allDay || !occurrence.startTime) return null
  const start = occurrence.startTime.slice(0, 5)
  return occurrence.endTime ? `${start} – ${occurrence.endTime.slice(0, 5)}` : start
}

/**
 * Everything on one day, grouped by where it came from, with enough detail per item to decide
 * without opening it — where it is, what to bring, when it starts.
 *
 * Grouping rather than a flat list because the sources answer different questions — what we are
 * doing, what we owe, what we are eating — and a reader scanning a busy Saturday finds the one
 * they care about faster when the kinds are kept apart. This is also where adding to that day
 * starts: the form opens already dated, so nobody retypes a date they just clicked.
 */
export function DayDetailModal({
  date, occurrences, canWrite, onCreateEvent, onSelectOccurrence, onClose,
}: DayDetailModalProps) {
  const { t } = useTranslation('calendar')
  const locale = resolveLocale(i18next.language)
  const dayOccurrences = groupByDay(occurrences, [date]).get(date) ?? []

  return (
    <Dialog open onClose={onClose} title={formatPeriodLabel('day', date, locale)} showCloseButton maxWidth="max-w-lg">
      <div className="flex flex-col gap-4">
        {dayOccurrences.length === 0 && <p className="text-sm text-fg-3">{t('empty_day')}</p>}

        {SOURCE_ORDER.map((source) => {
          const ofSource = dayOccurrences.filter((o) => o.source === source)
          if (ofSource.length === 0) return null
          return (
            <section key={source}>
              <h3 className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-fg-3">
                {t(`source.${source}`)}
              </h3>
              <ul className="flex flex-col gap-1.5">
                {ofSource.map((occurrence) => {
                  const multiDay = occurrence.startDate !== occurrence.endDate
                  const when = [
                    multiDay ? formatSpan(occurrence.startDate, occurrence.endDate, locale) : null,
                    timeSpan(occurrence) ?? (multiDay ? null : t('all_day_short')),
                  ].filter(Boolean).join(' · ')
                  return (
                    <li key={occurrence.sourceId}>
                      <button type="button" onClick={() => onSelectOccurrence(occurrence)}
                        className="flex w-full gap-2.5 rounded-xl border border-border bg-bg-2 px-3 py-2 text-left hover:border-border-2">
                        <span className={`mt-1.5 size-2 shrink-0 rounded-full ${dotClassFor(occurrence)}`} />
                        <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                          <span className="truncate text-sm font-semibold text-fg-0">{occurrence.title}</span>
                          <span className="text-xs tabular-nums text-fg-2">{when}</span>
                          {occurrence.location && (
                            <span data-testid="occurrence-location" className="flex items-center gap-1 text-xs text-fg-2">
                              <MapPin aria-hidden className="size-3 shrink-0" />
                              <span className="truncate">{occurrence.location}</span>
                            </span>
                          )}
                          {occurrence.description && (
                            // Clamped: this is an overview of the day. The full text is one tap away.
                            <span data-testid="occurrence-description"
                              className="line-clamp-3 whitespace-pre-line text-xs text-fg-3">
                              {occurrence.description}
                            </span>
                          )}
                        </span>
                      </button>
                    </li>
                  )
                })}
              </ul>
            </section>
          )
        })}

        {canWrite && (
          <div className="flex justify-end border-t border-border pt-3">
            <Button type="button" style={CTA_BUTTON_STYLE} onClick={() => onCreateEvent(date)}>
              <Plus className="size-4" /> {t('day_detail.add_event')}
            </Button>
          </div>
        )}
      </div>
    </Dialog>
  )
}
