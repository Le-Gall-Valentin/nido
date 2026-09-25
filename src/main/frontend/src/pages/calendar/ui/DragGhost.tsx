import type { CalendarOccurrence } from '@/entities/calendar'
import { tintClassFor } from '../lib/sourceAppearance'

/**
 * The item's title under the pointer — shown only while the pointer is off every slot, past an
 * edge waiting for the next period. Over a slot the item is drawn in the calendar itself, so this
 * stays mounted but invisible.
 */
export function DragGhost({ occurrence, visible }: { occurrence: CalendarOccurrence; visible: boolean }) {
  return (
    <div className={`pointer-events-none truncate rounded-lg px-2 py-1 text-xs font-semibold shadow-lg ${tintClassFor(occurrence)}
      ${visible ? '' : 'invisible'}`}>
      {occurrence.title}
    </div>
  )
}
