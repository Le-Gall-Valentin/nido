import { useRef } from 'react'
import i18next from 'i18next'
import { useDraggable, useDroppable } from '@dnd-kit/core'
import { MapPin } from 'lucide-react'
import type { CalendarOccurrence, ScheduleChange } from '@/entities/calendar'
import { resolveLocale, usePointerIsFine } from '@/shared/lib'
import type { DragData, DropData } from '../lib/dragTypes'
import { canReschedule } from '@/features/reschedule-occurrence'
import { tintClassFor } from '../lib/sourceAppearance'
import { layoutOverlaps, type Placed } from '../lib/overlapLayout'
import { formatTimeRange } from '../lib/periodLabel'
import { isBandOccurrence, segmentFor, type Segment } from '../lib/segments'
import { fadeClassFor, useDragPreview } from '../model/dragPreview'
import { HOUR_HEIGHT } from '../lib/timeMath'

export { HOUR_HEIGHT } from '../lib/timeMath'

/** Never let a block collapse to nothing: a zero-length event must still be tappable. */
const MIN_BLOCK_HEIGHT = 20
/** The same floor in minutes: what a block covers on screen is what it overlaps. */
const MIN_BLOCK_MINUTES = (MIN_BLOCK_HEIGHT / HOUR_HEIGHT) * 60

/** A share of the column's width, as CSS. Rounded, so a third does not print sixteen decimals. */
function percent(fraction: number): string {
  return `${+(fraction * 100).toFixed(4)}%`
}

interface HourColumnProps {
  /** The day this column draws: an overnight event shows only its own piece of it. */
  day: string
  /** Timed occurrences of one day, already filtered. */
  occurrences: CalendarOccurrence[]
  /** Off for a viewer: nothing they drag could be saved. */
  canWrite: boolean
  onSelectOccurrence: (occurrence: CalendarOccurrence) => void
  /** A time being picked out to add an event — drawn in each column it covers. */
  picked?: ScheduleChange | null
  /** A press on empty grid here, which may start picking out a time. */
  onPickStart?: (event: React.PointerEvent<HTMLElement>, day: string) => void
}

/**
 * One day's timed occurrences, positioned by the hour — and the drop target that turns a pointer
 * height into a time. The layer reads the column's live top through `column`, which already moves
 * with the scroller, so a scrolled grid needs no correction.
 */
export function HourColumn({ day, occurrences, canWrite, onSelectOccurrence, picked, onPickStart }: HourColumnProps) {
  const element = useRef<HTMLDivElement | null>(null)
  // While an item is dragged, its landing slot is drawn here, by the same segment rules as a saved
  // block — an overnight landing shows its piece on each day.
  const landing = useDragPreview()?.occurrence
  const landingSegment = landing && !isBandOccurrence(landing) ? segmentFor(landing, day) : null
  const pickedSegment = picked ? segmentFor(picked, day) : null
  // Overlapping events are cascaded across the column, each a little right of the last.
  const placed = layoutOverlaps(occurrences.flatMap((occurrence) => {
    const segment = segmentFor(occurrence, day)
    return segment ? [{
      item: { occurrence, segment },
      start: segment.startMinutes,
      end: Math.max(segment.endMinutes, segment.startMinutes + MIN_BLOCK_MINUTES),
    }] : []
  }))
  const { setNodeRef } = useDroppable({
    id: `hours:${day}`, disabled: !canWrite,
    data: { target: { kind: 'hours', day }, column: () => element.current } satisfies DropData,
  })
  return (
    <div ref={(node) => { element.current = node; setNodeRef(node) }} data-testid="hour-column" data-day={day}
      className="relative select-none" style={{ height: `${24 * HOUR_HEIGHT}px` }}
      onPointerDown={onPickStart && ((event) => onPickStart(event, day))}>
      {Array.from({ length: 24 }, (_, hour) => (
        <div key={hour} className="border-b border-border/60" style={{ height: `${HOUR_HEIGHT}px` }} />
      ))}
      {placed.map(({ item: { occurrence, segment }, ...placement }) => (
        <GridBlock key={`${occurrence.sourceId}-${day}`} occurrence={occurrence} day={day}
          segment={segment} placement={placement} canWrite={canWrite} column={() => element.current}
          onSelect={() => onSelectOccurrence(occurrence)} />
      ))}
      {landing && landingSegment && <LandingBlock occurrence={landing} segment={landingSegment} />}
      {picked && pickedSegment && <PickedBlock range={picked} segment={pickedSegment} />}
    </div>
  )
}

/** A time being picked out to add an event, with the times it would get. Inert, like a landing. */
function PickedBlock({ range, segment }: { range: ScheduleChange; segment: Segment }) {
  return (
    <div data-testid="grid-selection" style={boxOf(segment)}
      className="pointer-events-none absolute inset-x-0.5 z-20 overflow-clip rounded bg-accent-dim px-1 py-0.5 text-[11px] font-semibold text-accent ring-2 ring-accent">
      <div className="sticky top-0 tabular-nums">{formatTimeRange(range, resolveLocale(i18next.language))}</div>
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
          <div className="tabular-nums opacity-80">{formatTimeRange(occurrence, resolveLocale(i18next.language))}</div>
        </div>
      </div>
    </div>
  )
}

interface GridBlockProps {
  occurrence: CalendarOccurrence
  day: string
  segment: Segment
  /** Where across the column it sits, among the events it overlaps. */
  placement: Omit<Placed<unknown>, 'item'>
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
function GridBlock({ occurrence, day, segment, placement, canWrite, column, onSelect }: GridBlockProps) {
  const pointerIsFine = usePointerIsFine()
  const draggable = canWrite && canReschedule(occurrence)
  const fade = fadeClassFor(useDragPreview(), occurrence)
  const move = useDraggable({
    id: `grid:${occurrence.sourceId}:${day}`, disabled: !draggable,
    data: { intent: { kind: 'move', occurrence, from: 'grid', day }, column } satisfies DragData,
  })
  // A six-pixel strip is a mouse target; a finger resizes through the edit form instead.
  const resizable = draggable && pointerIsFine && occurrence.source === 'EVENT'
  return (
    // A later lane is drawn over an earlier one; the position is on the wrapper, the 2px inset in its padding.
    <div style={{ ...boxOf(segment), left: percent(placement.left), width: percent(placement.width), zIndex: placement.lane + 1 }}
      className={`absolute px-0.5 ${fade}`}>
      {resizable && segment.isStart && <ResizeHandle occurrence={occurrence} edge="start" />}
      <button ref={draggable ? move.setNodeRef : undefined}
        {...(draggable ? move.listeners : {})} {...(draggable ? move.attributes : {})}
        type="button" data-draggable={draggable || undefined} onClick={onSelect}
        // The title sits at the top and sticks while a long piece scrolls past. overflow-clip, not
        // overflow-hidden: hidden would make the block its own scroller and pin the title to it.
        // Among overlapping events, each is edged in the page's colour: two of the same colour stay two.
        className={`flex h-full w-full touch-manipulation flex-col justify-start overflow-clip rounded px-1 py-0.5 text-left text-[11px] font-medium ${tintClassFor(occurrence)} ${draggable ? 'cursor-grab' : ''} ${placement.lanes > 1 ? 'ring-2 ring-bg-1' : ''}`}>
        <BlockDetails occurrence={occurrence} minutes={Math.max(MIN_BLOCK_MINUTES, segment.endMinutes - segment.startMinutes)} />
      </button>
      {resizable && segment.isEnd && <ResizeHandle occurrence={occurrence} edge="end" />}
    </div>
  )
}

/** One line of a block's text, in pixels — every line is set to it, so lines can be counted. */
const LINE_PX = 15
/** The block's vertical padding (py-0.5), top and bottom. */
const PADDING_PX = 4

/**
 * What a block says: everything that fits, in whole lines. With one line, its title and its start;
 * with two, its times under the title; then its place, then as much of its description as the lines
 * left can hold. It sticks to the top while a long piece scrolls past.
 */
function BlockDetails({ occurrence, minutes }: { occurrence: CalendarOccurrence; minutes: number }) {
  const lines = Math.floor(((minutes / 60) * HOUR_HEIGHT - PADDING_PX) / LINE_PX)
  if (lines < 2) {
    return (
      <span className="sticky top-0 max-w-full truncate leading-[15px]">
        {occurrence.title}<span className="opacity-80"> · {occurrence.startTime?.slice(0, 5)}</span>
      </span>
    )
  }
  const place = lines >= 3 ? occurrence.location : null
  const linesLeft = lines - 2 - (place ? 1 : 0)
  const description = linesLeft > 0 ? occurrence.description : null
  return (
    <div className="sticky top-0 flex w-full min-w-0 flex-col leading-[15px]">
      <span className="truncate">{occurrence.title}</span>
      <span className="truncate tabular-nums opacity-80">{formatTimeRange(occurrence, resolveLocale(i18next.language))}</span>
      {place && (
        <span className="flex min-w-0 items-center gap-1 opacity-80">
          <MapPin aria-hidden className="size-3 shrink-0" />
          <span className="truncate">{place}</span>
        </span>
      )}
      {description && (
        // Whole lines only: the height is a number of lines, never a line cut in half.
        <span className="overflow-hidden whitespace-pre-line break-words font-normal opacity-80"
          style={{ maxHeight: `${linesLeft * LINE_PX}px` }}>
          {description}
        </span>
      )}
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
