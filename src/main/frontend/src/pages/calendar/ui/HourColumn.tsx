import { useRef } from 'react'
import { useDraggable, useDroppable } from '@dnd-kit/core'
import type { CalendarOccurrence } from '@/entities/calendar'
import { usePointerIsFine } from '@/shared/lib'
import type { DragData, DropData } from '../lib/dragTypes'
import { isDraggable } from '../lib/isDraggable'
import { tintClassFor } from '../lib/sourceAppearance'
import { isBandOccurrence, segmentFor, type Segment } from '../lib/segments'
import { fadeClassFor, useDragPreview } from '../model/dragPreview'
import { HOUR_HEIGHT } from '../lib/timeMath'

export { HOUR_HEIGHT } from '../lib/timeMath'

/** Never let a block collapse to nothing: a zero-length event must still be tappable. */
const MIN_BLOCK_HEIGHT = 20

interface HourColumnProps {
  /** The day this column draws: an overnight event shows only its own piece of it. */
  day: string
  /** Timed occurrences of one day, already filtered. */
  occurrences: CalendarOccurrence[]
  /** Off for a viewer: nothing they drag could be saved. */
  canWrite: boolean
  onSelectOccurrence: (occurrence: CalendarOccurrence) => void
}

/**
 * One day's timed occurrences, positioned by the hour — and the drop target that turns a pointer
 * height into a time. The layer reads the column's live top through `column`, which already moves
 * with the scroller, so a scrolled grid needs no correction.
 */
export function HourColumn({ day, occurrences, canWrite, onSelectOccurrence }: HourColumnProps) {
  const element = useRef<HTMLDivElement | null>(null)
  // While an item is dragged, its landing slot is drawn here, by the same segment rules as a saved
  // block — an overnight landing shows its piece on each day.
  const landing = useDragPreview()?.occurrence
  const landingSegment = landing && !isBandOccurrence(landing) ? segmentFor(landing, day) : null
  const { setNodeRef } = useDroppable({
    id: `hours:${day}`, disabled: !canWrite,
    data: { target: { kind: 'hours', day }, column: () => element.current } satisfies DropData,
  })
  return (
    <div ref={(node) => { element.current = node; setNodeRef(node) }} data-testid="hour-column" className="relative"
      style={{ height: `${24 * HOUR_HEIGHT}px` }}>
      {Array.from({ length: 24 }, (_, hour) => (
        <div key={hour} className="border-b border-border/60" style={{ height: `${HOUR_HEIGHT}px` }} />
      ))}
      {occurrences.map((occurrence) => {
        const segment = segmentFor(occurrence, day)
        return segment && (
          <GridBlock key={`${occurrence.sourceId}-${day}`} occurrence={occurrence} day={day}
            segment={segment} canWrite={canWrite} column={() => element.current}
            onSelect={() => onSelectOccurrence(occurrence)} />
        )
      })}
      {landing && landingSegment && <LandingBlock occurrence={landing} segment={landingSegment} />}
    </div>
  )
}

/** A segment's place in the column, in pixels. */
function boxOf(segment: Segment): { top: string; height: string } {
  const top = (segment.startMinutes / 60) * HOUR_HEIGHT
  const height = Math.max(MIN_BLOCK_HEIGHT, ((segment.endMinutes - segment.startMinutes) / 60) * HOUR_HEIGHT)
  return { top: `${top}px`, height: `${height}px` }
}

/**
 * Where a dragged item would land: drawn solid over the grid, with the times it would get. Inert —
 * the pointer and the drop targets see straight through it.
 */
function LandingBlock({ occurrence, segment }: { occurrence: CalendarOccurrence; segment: Segment }) {
  return (
    <div data-testid="drag-preview" style={boxOf(segment)}
      className="pointer-events-none absolute inset-x-0.5 z-20 rounded bg-bg-1 shadow-lg ring-2 ring-accent">
      <div className={`h-full overflow-clip rounded px-1 py-0.5 text-[11px] font-medium ${tintClassFor(occurrence)}`}>
        <div className="sticky top-0">
          <div className="truncate">{occurrence.title}</div>
          <div className="tabular-nums opacity-80">
            {occurrence.startTime?.slice(0, 5)} – {occurrence.endTime?.slice(0, 5)}
          </div>
        </div>
      </div>
    </div>
  )
}

interface GridBlockProps {
  occurrence: CalendarOccurrence
  day: string
  segment: Segment
  canWrite: boolean
  /** The column this piece sits in: the layer reads the grab time against its live top. */
  column: () => HTMLElement | null
  onSelect: () => void
}

/**
 * One piece of an occurrence in the grid. The wrapper carries the position because the resize
 * handles share its box; they sit only on the event's true start and true end, so an overnight
 * event is stretched from its first piece's top and its last piece's bottom — never from midnight.
 */
function GridBlock({ occurrence, day, segment, canWrite, column, onSelect }: GridBlockProps) {
  const pointerIsFine = usePointerIsFine()
  const draggable = canWrite && isDraggable(occurrence)
  const fade = fadeClassFor(useDragPreview(), occurrence)
  const move = useDraggable({
    id: `grid:${occurrence.sourceId}:${day}`, disabled: !draggable,
    data: { intent: { kind: 'move', occurrence, from: 'grid', day }, column } satisfies DragData,
  })
  // A six-pixel strip is a mouse target; a finger resizes through the edit form instead.
  const resizable = draggable && pointerIsFine && occurrence.source === 'EVENT'
  return (
    <div style={boxOf(segment)} className={`absolute inset-x-0.5 ${fade}`}>
      {resizable && segment.isStart && <ResizeHandle occurrence={occurrence} edge="start" />}
      <button ref={draggable ? move.setNodeRef : undefined}
        {...(draggable ? move.listeners : {})} {...(draggable ? move.attributes : {})}
        type="button" data-draggable={draggable || undefined} onClick={onSelect}
        // The title sits at the top and sticks while a long piece scrolls past. overflow-clip, not
        // overflow-hidden: hidden would make the block its own scroller and pin the title to it.
        className={`flex h-full w-full touch-manipulation flex-col justify-start overflow-clip rounded px-1 py-0.5 text-left text-[11px] font-medium ${tintClassFor(occurrence)} ${draggable ? 'cursor-grab' : ''}`}>
        <span className="sticky top-0 max-w-full truncate">{occurrence.title}</span>
      </button>
      {resizable && segment.isEnd && <ResizeHandle occurrence={occurrence} edge="end" />}
    </div>
  )
}

function ResizeHandle({ occurrence, edge }: { occurrence: CalendarOccurrence; edge: 'start' | 'end' }) {
  const { setNodeRef, listeners, attributes } = useDraggable({
    id: `resize-${edge}:${occurrence.sourceId}`,
    data: { intent: { kind: edge === 'start' ? 'resize-start' : 'resize-end', occurrence } } satisfies DragData,
  })
  return (
    // A mouse target only: out of the tab order and hidden from screen readers, which reach the
    // same change through the edit form.
    <div ref={setNodeRef} {...listeners} {...attributes} tabIndex={-1} aria-hidden="true" data-testid={`resize-${edge}`}
      className={`absolute inset-x-1 z-10 h-1.5 cursor-ns-resize ${edge === 'start' ? 'top-0' : 'bottom-0'}`} />
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
