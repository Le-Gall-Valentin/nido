import { useCallback, useMemo, useRef } from 'react'

/** Minimum horizontal travel, in pixels, before a gesture counts as a swipe. */
const MIN_DISTANCE = 60
/** Above this, the gesture was a drag or a slow scrub — not a flick. */
const MAX_DURATION_MS = 250
/** A gesture more vertical than this belongs to the scroller. */
const MAX_VERTICAL_RATIO = 0.6

interface Pointer { x: number; y: number; at: number }

/**
 * Flick left or right to change period — on touch only.
 *
 * Three gestures share the same surface: a short flick changes the period, a drag reschedules a
 * chip, and anything else scrolls. Keeping them apart takes three rules:
 *
 * - A mouse never swipes. It has the arrows, nobody flicks with one, and a fast mouse drag of a
 *   chip is exactly the shape of a flick — it used to page the month away in the middle of a drop.
 * - A drag that starts takes the gesture over: the page calls `cancel` from dnd-kit's drag start,
 *   which always comes before the pointer is released, so no listener-order assumption is needed.
 * - Duration and direction caps separate a flick from a long press or a diagonal scroll.
 */
export function useSwipePeriod(onShift: (direction: -1 | 1) => void) {
  const start = useRef<Pointer | null>(null)

  const onPointerDown = useCallback((event: React.PointerEvent) => {
    start.current = event.pointerType === 'mouse'
      ? null
      : { x: event.clientX, y: event.clientY, at: Date.now() }
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

  const cancel = useCallback(() => { start.current = null }, [])

  const handlers = useMemo(
    () => ({ onPointerDown, onPointerUp, onPointerCancel: cancel }),
    [onPointerDown, onPointerUp, cancel])

  return { handlers, cancel }
}
