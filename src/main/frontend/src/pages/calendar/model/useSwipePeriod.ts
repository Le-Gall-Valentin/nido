import { useCallback, useRef } from 'react'

/** Minimum horizontal travel, in pixels, before a gesture counts as a swipe. */
const MIN_DISTANCE = 60
/** Above this, the gesture was a drag or a slow scrub — not a flick. */
const MAX_DURATION_MS = 250
/** A gesture more vertical than this belongs to the scroller. */
const MAX_VERTICAL_RATIO = 0.6

interface Pointer { x: number; y: number; at: number }

/**
 * Flick left or right to change period.
 *
 * Deliberately narrow, because three gestures share the same surface: a short horizontal flick
 * changes the period, a long press starts a drag (dnd-kit's own activation constraint), and
 * anything else scrolls. The thresholds are what keep those three apart — without the duration
 * cap a slow drag would also page the month, and without the vertical ratio a diagonal scroll
 * would.
 */
export function useSwipePeriod(onShift: (direction: -1 | 1) => void) {
  const start = useRef<Pointer | null>(null)

  const onPointerDown = useCallback((event: React.PointerEvent) => {
    start.current = { x: event.clientX, y: event.clientY, at: Date.now() }
  }, [])

  const onPointerUp = useCallback((event: React.PointerEvent) => {
    const from = start.current
    start.current = null
    if (!from) return

    const dx = event.clientX - from.x
    const dy = event.clientY - from.y
    const elapsed = Date.now() - from.at

    if (elapsed > MAX_DURATION_MS) return
    if (Math.abs(dx) < MIN_DISTANCE) return
    if (Math.abs(dy) > Math.abs(dx) * MAX_VERTICAL_RATIO) return

    onShift(dx < 0 ? 1 : -1)
  }, [onShift])

  const onPointerCancel = useCallback(() => { start.current = null }, [])

  return { onPointerDown, onPointerUp, onPointerCancel }
}
