import { useCallback, useEffect, useRef, useState } from 'react'
import { IDLE, stepDwell, type DwellState, type Zone } from '../lib/edgeDwell'

/** How often the countdown is re-checked while armed — the pointer may hold perfectly still. */
const TICK_MS = 100

export function useEdgeNavigation(onShift: (direction: -1 | 1) => void) {
  // The latest callback, read at fire time. The page's shiftPeriod changes with the date, and a
  // countdown that kept the first one would ask for the same period on every repeat.
  const onShiftRef = useRef(onShift)
  useEffect(() => { onShiftRef.current = onShift }, [onShift])

  const state = useRef<DwellState>(IDLE)
  const zone = useRef<Zone>(0)
  const timer = useRef<ReturnType<typeof setInterval> | null>(null)
  const [armed, setArmed] = useState<Zone>(0)

  const tick = useCallback(() => {
    const result = stepDwell(state.current, zone.current, Date.now())
    state.current = result.state
    if (result.fire !== 0) onShiftRef.current(result.fire)
  }, [])

  const stop = useCallback(() => {
    if (timer.current !== null) clearInterval(timer.current)
    timer.current = null
    zone.current = 0
    state.current = IDLE
    setArmed(0)
  }, [])

  const update = useCallback((next: Zone) => {
    if (next === 0) { stop(); return }
    zone.current = next
    setArmed(next)
    if (timer.current === null) timer.current = setInterval(tick, TICK_MS)
    tick()
  }, [stop, tick])

  useEffect(() => stop, [stop])

  return { armed, update, stop }
}
