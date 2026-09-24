import { useTranslation } from 'react-i18next'
import type { Zone } from '../lib/edgeDwell'
import type { CalendarView } from '../lib/calendarWindow'

/** Shown on the armed edge, so a period change never comes as a surprise. */
export function EdgeCue({ armed, view, vertical }: { armed: Zone; view: CalendarView; vertical: boolean }) {
  const { t } = useTranslation('calendar')
  if (armed === 0) return null
  const key = `drag.${armed > 0 ? 'next' : 'prev'}_${view}${vertical ? '_vertical' : ''}`
  const place = vertical
    ? (armed > 0 ? 'bottom-2 left-1/2 -translate-x-1/2' : 'top-2 left-1/2 -translate-x-1/2')
    : (armed > 0 ? 'right-2 top-1/2 -translate-y-1/2' : 'left-2 top-1/2 -translate-y-1/2')
  return (
    // Vertical cues sit on the viewport, since the phone list is taller than the screen; horizontal
    // cues sit on the calendar's own edges.
    <div role="status" className={`pointer-events-none z-50 rounded-full bg-accent px-3 py-1.5 text-xs font-semibold text-white shadow-lg ${vertical ? 'fixed' : 'absolute'} ${place}`}>
      {t(key)}
    </div>
  )
}
