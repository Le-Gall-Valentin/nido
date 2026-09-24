import { useCallback, useMemo, useRef } from 'react'

/** Minimum horizontal travel, in pixels, before a gesture counts as a swipe. */
const MIN_DISTANCE = 60
/** Above this, the gesture was a drag or a slow scrub — not a flick. */
const MAX_DURATION_MS = 250
/** A gesture more vertical than this belongs to the scroller. */
const MAX_VERTICAL_RATIO = 0.6

interface Touchdown { x: number; y: number; at: number }

/**
 * Flick left or right to change period — on touch only.
 *
 * Read from touch events, not pointer events: a flick is a pan to the browser, and Chrome answers a
 * pan with pointercancel, so a swipe built on pointers never fired on a real phone. Touches end
 * normally. A mouse sends no touches, so it can never swipe — it has the arrows, and a fast mouse
 * drag of a chip is exactly the shape of a flick.
 *
 * Three gestures share the same surface: a short flick changes the period, a drag reschedules an
 * item, and anything else scrolls. Keeping them apart takes three rules:
 *
 * - One finger only: two are a pinch.
 * - A drag that starts takes the gesture over: the page calls `cancel` from dnd-kit's drag start,
 *   which always comes before the finger lifts, so no listener-order assumption is needed.
 * - Duration and direction caps separate a flick from a long press or a diagonal scroll.
 */
export function useSwipePeriod(onShift: (direction: -1 | 1) => void) {
  const start = useRef<Touchdown | null>(null)

  const onTouchStart = useCallback((event: React.TouchEvent) => {
    const touch = event.touches[0]
    start.current = event.touches.length === 1 && touch
      ? { x: touch.clientX, y: touch.clientY, at: Date.now() }
      : null
  }, [])

  const onTouchEnd = useCallback((event: React.TouchEvent) => {
    const from = start.current
    start.current = null
    const touch = event.changedTouches[0]
    if (!from || !touch) return

    const dx = touch.clientX - from.x
    const dy = touch.clientY - from.y
    const elapsed = Date.now() - from.at

    if (elapsed > MAX_DURATION_MS) return
    if (Math.abs(dx) < MIN_DISTANCE) return
    if (Math.abs(dy) > Math.abs(dx) * MAX_VERTICAL_RATIO) return

    onShift(dx < 0 ? 1 : -1)
  }, [onShift])

  const cancel = useCallback(() => { start.current = null }, [])

  const handlers = useMemo(
    () => ({ onTouchStart, onTouchEnd, onTouchCancel: cancel }),
    [onTouchStart, onTouchEnd, cancel])

  return { handlers, cancel }
}
