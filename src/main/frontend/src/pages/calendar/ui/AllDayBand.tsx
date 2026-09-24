import { useTranslation } from 'react-i18next'
import { useDraggable, useDroppable } from '@dnd-kit/core'
import type { CalendarOccurrence } from '@/entities/calendar'
import type { DragData, DropData } from '../lib/dragTypes'
import { isDraggable } from '../lib/isDraggable'
import { covers, isBandOccurrence } from '../lib/segments'
import { fadeClassFor, useDragPreview } from '../model/dragPreview'
import { tintClassFor } from '../lib/sourceAppearance'

interface AllDayBandProps {
  day: string
  occurrences: CalendarOccurrence[]
  /** Off for a viewer: nothing they drag could be saved. */
  canWrite: boolean
  onSelectOccurrence: (occurrence: CalendarOccurrence) => void
  /** Part of the days being picked out to add an all-day event. */
  picked?: boolean
  /** A press on empty band here, which may start picking out days. */
  onPickStart?: (event: React.PointerEvent<HTMLElement>, day: string) => void
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
export function AllDayBand({ day, occurrences, canWrite, onSelectOccurrence, picked = false, onPickStart }: AllDayBandProps) {
  const { t } = useTranslation('calendar')
  const { setNodeRef, isOver } = useDroppable({
    id: `all-day:${day}`, disabled: !canWrite, data: { target: { kind: 'all-day', day } } satisfies DropData,
  })
  // A dragged item landing here, all-day or longer than a day, is drawn in the band of each day it covers.
  const landing = useDragPreview()?.occurrence
  const landsHere = landing && isBandOccurrence(landing) && covers(landing, day)
  return (
    <div ref={setNodeRef} data-testid="all-day-band" data-day={day} data-selected={picked || undefined}
      onPointerDown={onPickStart && ((event) => onPickStart(event, day))}
      className={`flex min-h-7 select-none flex-wrap gap-1 border-b border-border px-1 py-1
        ${isOver || picked ? 'bg-accent-dim' : ''} ${picked ? 'ring-2 ring-inset ring-accent' : ''}`}>
      <span className="sr-only">{t('all_day_band')}</span>
      {occurrences.map((occurrence) => (
        <BandItem key={`${occurrence.sourceId}-${day}`} occurrence={occurrence} day={day} canWrite={canWrite}
          onSelect={() => onSelectOccurrence(occurrence)} />
      ))}
      {landsHere && (
        <span data-testid="drag-preview"
          className={`pointer-events-none truncate rounded px-1.5 py-0.5 text-[11px] font-medium shadow-md ring-2 ring-accent ${tintClassFor(landing)}`}>
          {landing.title}
        </span>
      )}
    </div>
  )
}

function BandItem({ occurrence, day, canWrite, onSelect }: {
  occurrence: CalendarOccurrence; day: string; canWrite: boolean; onSelect: () => void
}) {
  const draggable = canWrite && isDraggable(occurrence)
  const fade = fadeClassFor(useDragPreview(), occurrence)
  const { setNodeRef, listeners, attributes } = useDraggable({
    id: `band:${occurrence.sourceId}:${day}`, disabled: !draggable,
    data: { intent: { kind: 'move', occurrence, from: 'band', day } } satisfies DragData,
  })
  return (
    <button ref={draggable ? setNodeRef : undefined} {...(draggable ? listeners : {})} {...(draggable ? attributes : {})}
      type="button" data-draggable={draggable || undefined} onClick={onSelect}
      className={`touch-manipulation truncate rounded px-1.5 py-0.5 text-[11px] font-medium ${tintClassFor(occurrence)}
        ${draggable ? 'cursor-grab' : ''} ${fade}`}>
      {occurrence.title}
    </button>
  )
}
