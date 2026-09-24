import { useTranslation } from 'react-i18next'
import { useDraggable, useDroppable } from '@dnd-kit/core'
import type { CalendarOccurrence } from '@/entities/calendar'
import type { DragData, DropData } from '../lib/dragTypes'
import { isDraggable } from '../lib/isDraggable'
import { tintClassFor } from '../lib/sourceAppearance'

interface AllDayBandProps {
  day: string
  occurrences: CalendarOccurrence[]
  /** Off for a viewer: nothing they drag could be saved. */
  canWrite: boolean
  onSelectOccurrence: (occurrence: CalendarOccurrence) => void
}

/**
 * Where everything without a time goes, one band per day.
 *
 * Not just all-day events: a due task, a planned meal, a recurring debit and a savings deadline
 * have no hour either. Without this band each of them would have to be shown at an invented time,
 * which is the reason the all-day flag exists at all.
 *
 * It renders even when empty — an empty band is still where a timed event is dropped to become
 * all-day, so it must stay a target.
 */
export function AllDayBand({ day, occurrences, canWrite, onSelectOccurrence }: AllDayBandProps) {
  const { t } = useTranslation('calendar')
  const { setNodeRef, isOver } = useDroppable({
    id: `all-day:${day}`, disabled: !canWrite, data: { target: { kind: 'all-day', day } } satisfies DropData,
  })
  return (
    <div ref={setNodeRef} data-testid="all-day-band"
      className={`flex min-h-7 flex-wrap gap-1 border-b border-border px-1 py-1 ${isOver ? 'bg-accent-dim' : ''}`}>
      <span className="sr-only">{t('all_day_band')}</span>
      {occurrences.map((occurrence) => (
        <BandItem key={`${occurrence.sourceId}-${day}`} occurrence={occurrence} day={day} canWrite={canWrite}
          onSelect={() => onSelectOccurrence(occurrence)} />
      ))}
    </div>
  )
}

function BandItem({ occurrence, day, canWrite, onSelect }: {
  occurrence: CalendarOccurrence; day: string; canWrite: boolean; onSelect: () => void
}) {
  const draggable = canWrite && isDraggable(occurrence)
  const { setNodeRef, listeners, attributes, isDragging } = useDraggable({
    id: `band:${occurrence.sourceId}:${day}`, disabled: !draggable,
    data: { intent: { kind: 'move', occurrence, from: 'band', day } } satisfies DragData,
  })
  return (
    <button ref={draggable ? setNodeRef : undefined} {...(draggable ? listeners : {})} {...(draggable ? attributes : {})}
      type="button" data-draggable={draggable || undefined} onClick={onSelect}
      className={`touch-manipulation truncate rounded px-1.5 py-0.5 text-[11px] font-medium ${tintClassFor(occurrence)}
        ${draggable ? 'cursor-grab' : ''} ${isDragging ? 'opacity-40' : ''}`}>
      {occurrence.title}
    </button>
  )
}
