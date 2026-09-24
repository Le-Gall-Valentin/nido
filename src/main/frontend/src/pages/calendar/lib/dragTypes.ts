import type { CalendarOccurrence } from '@/entities/calendar'

export type DragOrigin = 'cell' | 'band' | 'grid' | 'row'

export type DragIntent =
  | { kind: 'move'; occurrence: CalendarOccurrence; from: DragOrigin }
  | { kind: 'resize-start' | 'resize-end'; occurrence: CalendarOccurrence }

export type DropTarget =
  | { kind: 'day'; day: string } | { kind: 'all-day'; day: string } | { kind: 'hours'; day: string }

export interface ScheduleChange {
  allDay: boolean; startDate: string; startTime: string | null; endDate: string; endTime: string | null
}

/** What a droppable carries in dnd-kit `data`: its target and, for an hour column, its live element. */
export interface DropData { target: DropTarget; column?: () => HTMLElement | null }

/** What a draggable carries. The grab offset of a grid block is read from dnd-kit's own start rect. */
export interface DragData { intent: DragIntent }
