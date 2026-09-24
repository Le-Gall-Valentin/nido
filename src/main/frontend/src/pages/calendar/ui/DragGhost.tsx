import { useTranslation } from 'react-i18next'
import i18next from 'i18next'
import { resolveLocale } from '@/shared/lib'
import type { DragIntent, ScheduleChange } from '../lib/dragTypes'
import { daysBetween } from '../lib/calendarWindow'
import { formatShortDay } from '../lib/periodLabel'
import { DAY_MINUTES, timeToMinutes } from '../lib/timeMath'
import { tintClassFor } from '../lib/sourceAppearance'

function duration(change: ScheduleChange): number {
  if (change.allDay || !change.startTime || !change.endTime) return 0
  return daysBetween(change.startDate, change.endDate) * DAY_MINUTES
    + timeToMinutes(change.endTime) - timeToMinutes(change.startTime)
}

/** Follows the pointer and says, live, where the item will land. */
export function DragGhost({ intent, preview }: { intent: DragIntent; preview: ScheduleChange | null }) {
  const { t } = useTranslation('calendar')
  const locale = resolveLocale(i18next.language)
  const when = !preview ? null
    : preview.allDay ? `${formatShortDay(preview.startDate, locale)} · ${t('all_day_short')}`
    : intent.kind === 'move'
      ? `${formatShortDay(preview.startDate, locale)} · ${preview.startTime} – ${preview.endTime}`
      : `${preview.startTime} – ${preview.endTime} (${t('drag.duration', {
          hours: Math.floor(duration(preview) / 60), minutes: duration(preview) % 60 })})`
  return (
    <div className={`pointer-events-none rounded-lg px-2 py-1 text-xs font-semibold shadow-lg ${tintClassFor(intent.occurrence)}`}>
      <div className="truncate">{intent.occurrence.title}</div>
      {when && <div className="tabular-nums opacity-80">{when}</div>}
    </div>
  )
}
