import { useMemo, type ReactNode } from 'react'
import { DragPreviewContext, toPreview, type DragPreviewState } from './dragPreview'

/**
 * Hands the views what to draw while an item is dragged. The landing place is drawn by each view
 * with its own layout rules — the same ones the saved item will be drawn with — so the slot shown
 * during the drag is exactly the slot the drop writes.
 */
export function DragPreviewProvider({ value, children }: { value: DragPreviewState | null; children: ReactNode }) {
  const preview = useMemo(() => toPreview(value), [value])
  return <DragPreviewContext.Provider value={preview}>{children}</DragPreviewContext.Provider>
}
