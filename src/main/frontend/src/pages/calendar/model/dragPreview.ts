import { createContext, useContext } from 'react'
import type { CalendarOccurrence, ScheduleChange } from '@/entities/calendar'
import { isUnchanged } from '../lib/dragResolution'
import type { DragIntent } from '../lib/dragTypes'

/** What the drag layer knows during a drag: what is dragged, and where it would land now. */
export interface DragPreviewState { intent: DragIntent; change: ScheduleChange | null }

export interface DragPreview {
  /** The dragged occurrence: every piece of it on screen is dimmed or hidden meanwhile. */
  draggedId: string
  /** Dimmed while it moves, hidden while it stretches (the copy is the stretch), as-is at home. */
  fade: 'dim' | 'hide' | null
  /** The occurrence at its landing place, for the views to draw — null at home or off any slot. */
  occurrence: CalendarOccurrence | null
}

export const DragPreviewContext = createContext<DragPreview | null>(null)

/** What the views draw for a drag state — see DragPreviewProvider. */
export function toPreview(state: DragPreviewState | null): DragPreview | null {
  if (!state) return null
  const { intent, change } = state
  const moving = intent.kind === 'move'
  const draggedId = intent.occurrence.sourceId
  if (change === null) return { draggedId, fade: moving ? 'dim' : null, occurrence: null }
  if (isUnchanged(intent.occurrence, change)) return { draggedId, fade: null, occurrence: null }
  return { draggedId, fade: moving ? 'dim' : 'hide', occurrence: { ...intent.occurrence, ...change } }
}

export function useDragPreview(): DragPreview | null {
  return useContext(DragPreviewContext)
}

/** The class an occurrence's own pieces take during a drag. Literal names, for Tailwind to keep. */
export function fadeClassFor(preview: DragPreview | null, occurrence: CalendarOccurrence): string {
  if (!preview || preview.draggedId !== occurrence.sourceId) return ''
  if (preview.fade === 'dim') return 'opacity-40'
  if (preview.fade === 'hide') return 'opacity-0'
  return ''
}
