import type { CalendarOccurrence } from '@/entities/calendar'

export type DragOrigin = 'cell' | 'band' | 'grid' | 'row'

/**
 * A move is anchored on the piece that was grabbed — its `day`, and in the hour grid the time under
 * the pointer when it was grabbed (`grabMinutes`, filled in by the layer). A trip grabbed by its
 * Wednesday and dropped on Thursday moves one day; put back, it has not moved at all.
 */
export type DragIntent =
  | { kind: 'move'; occurrence: CalendarOccurrence; from: DragOrigin; day: string; grabMinutes?: number }
  | { kind: 'resize-start' | 'resize-end'; occurrence: CalendarOccurrence }

export type DropTarget =
  | { kind: 'day'; day: string } | { kind: 'all-day'; day: string } | { kind: 'hours'; day: string }

/** What a droppable carries in dnd-kit `data`: its target and, for an hour column, its live element. */
export interface DropData { target: DropTarget; column?: () => HTMLElement | null }

/** What a draggable carries: its intent and, for a grid block, the live column it sits in. */
export interface DragData { intent: DragIntent; column?: () => HTMLElement | null }
