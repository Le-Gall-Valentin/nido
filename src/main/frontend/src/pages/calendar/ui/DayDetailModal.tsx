import { useTranslation } from 'react-i18next'
import { Dialog } from '@/shared/ui'
import type { CalendarOccurrence } from '@/entities/calendar'
import { groupByDay } from '../lib/calendarWindow'
import { SOURCE_ORDER, tintClassFor } from '../lib/sourceAppearance'

interface DayDetailModalProps {
  date: string
  occurrences: CalendarOccurrence[]
  onSelectOccurrence: (occurrence: CalendarOccurrence) => void
  onClose: () => void
}

/**
 * Everything on one day, grouped by where it came from.
 *
 * Grouping rather than a flat list because the sources answer different questions — what we are
 * doing, what we owe, what we are eating — and a reader scanning a busy Saturday finds the one
 * they care about faster when the kinds are kept apart.
 */
export function DayDetailModal({ date, occurrences, onSelectOccurrence, onClose }: DayDetailModalProps) {
  const { t } = useTranslation('calendar')
  const dayOccurrences = groupByDay(occurrences, [date]).get(date) ?? []

  return (
    <Dialog open onClose={onClose} title={t('day_detail.title', { date })} showCloseButton maxWidth="max-w-md">
      {dayOccurrences.length === 0 ? (
        <p className="text-sm text-fg-3">{t('empty_day')}</p>
      ) : (
        <div className="flex flex-col gap-3">
          {SOURCE_ORDER.map((source) => {
            const ofSource = dayOccurrences.filter((o) => o.source === source)
            if (ofSource.length === 0) return null
            return (
              <section key={source}>
                <h3 className="mb-1 text-xs font-semibold uppercase tracking-wide text-fg-3">
                  {t(`source.${source}`)}
                </h3>
                <ul className="flex flex-col gap-1">
                  {ofSource.map((occurrence) => (
                    <li key={occurrence.sourceId}>
                      <button type="button" onClick={() => onSelectOccurrence(occurrence)}
                        className={`flex w-full items-center gap-2 rounded px-2 py-1.5 text-left text-sm font-medium ${tintClassFor(occurrence)}`}>
                        <span className="shrink-0 tabular-nums text-xs">
                          {occurrence.allDay ? t('all_day_short') : occurrence.startTime?.slice(0, 5)}
                        </span>
                        <span className="truncate">{occurrence.title}</span>
                      </button>
                    </li>
                  ))}
                </ul>
              </section>
            )
          })}
        </div>
      )}
    </Dialog>
  )
}
