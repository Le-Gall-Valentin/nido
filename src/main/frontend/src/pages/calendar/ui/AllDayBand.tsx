import { useTranslation } from 'react-i18next'
import type { CalendarOccurrence } from '@/entities/calendar'
import { tintClassFor } from '../lib/sourceAppearance'

interface AllDayBandProps {
  occurrences: CalendarOccurrence[]
  onSelectOccurrence: (occurrence: CalendarOccurrence) => void
}

/**
 * Where everything without a time goes.
 *
 * Not just all-day events: a due task, a planned meal, a recurring debit and a savings deadline
 * have no hour either. Without this band each of them would have to be shown at an invented time,
 * which is the reason the all-day flag exists at all.
 */
export function AllDayBand({ occurrences, onSelectOccurrence }: AllDayBandProps) {
  const { t } = useTranslation('calendar')
  if (occurrences.length === 0) return null

  return (
    <div data-testid="all-day-band" className="flex flex-wrap gap-1 border-b border-border px-1 py-1.5">
      <span className="sr-only">{t('all_day_band')}</span>
      {occurrences.map((occurrence) => (
        <button
          key={occurrence.sourceId}
          type="button"
          onClick={() => onSelectOccurrence(occurrence)}
          className={`truncate rounded px-1.5 py-0.5 text-[11px] font-medium ${tintClassFor(occurrence)}`}
        >
          {occurrence.title}
        </button>
      ))}
    </div>
  )
}
