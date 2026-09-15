import type { CalendarOccurrence } from '@/entities/calendar'
import { tintClassFor } from '../lib/sourceAppearance'

/** Pixels per hour. Also the unit every block's top and height are computed in. */
export const HOUR_HEIGHT = 60

/** Never let a block collapse to nothing: a zero-length event must still be tappable. */
const MIN_BLOCK_HEIGHT = 20

function minutesFrom(time: string): number {
  const [hours, minutes] = time.split(':')
  return Number(hours) * 60 + Number(minutes)
}

interface HourColumnProps {
  /** Timed occurrences of one day, already filtered. */
  occurrences: CalendarOccurrence[]
  onSelectOccurrence: (occurrence: CalendarOccurrence) => void
}

/** One day's timed occurrences, positioned by the hour. */
export function HourColumn({ occurrences, onSelectOccurrence }: HourColumnProps) {
  return (
    <div className="relative" style={{ height: `${24 * HOUR_HEIGHT}px` }}>
      {Array.from({ length: 24 }, (_, hour) => (
        <div key={hour} className="border-b border-border/60" style={{ height: `${HOUR_HEIGHT}px` }} />
      ))}
      {occurrences.map((occurrence) => {
        const start = occurrence.startTime ? minutesFrom(occurrence.startTime) : 0
        const end = occurrence.endTime ? minutesFrom(occurrence.endTime) : start
        const top = (start / 60) * HOUR_HEIGHT
        const height = Math.max(MIN_BLOCK_HEIGHT, ((end - start) / 60) * HOUR_HEIGHT)
        return (
          <button
            key={occurrence.sourceId}
            type="button"
            onClick={() => onSelectOccurrence(occurrence)}
            style={{ top: `${top}px`, height: `${height}px` }}
            className={`absolute inset-x-0.5 overflow-hidden rounded px-1 py-0.5 text-left text-[11px] font-medium ${tintClassFor(occurrence)}`}
          >
            <span className="truncate">{occurrence.title}</span>
          </button>
        )
      })}
    </div>
  )
}

/** The hour labels running down the left of a grid. */
export function HourGutter() {
  return (
    <div className="w-10 shrink-0">
      {Array.from({ length: 24 }, (_, hour) => (
        <div key={hour} className="relative border-b border-transparent" style={{ height: `${HOUR_HEIGHT}px` }}>
          <span className="absolute -top-1.5 right-1 text-[10px] tabular-nums text-fg-3">
            {hour === 0 ? '' : `${String(hour).padStart(2, '0')}:00`}
          </span>
        </div>
      ))}
    </div>
  )
}
