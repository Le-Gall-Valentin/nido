import { useEffect, useRef } from 'react'
import { openingMinutes } from '../lib/openingTime'
import { HOUR_HEIGHT } from '../lib/timeMath'

/** What the reader does to scroll or act in the grid: from then on, it is theirs. */
const READER_INPUT = ['wheel', 'touchstart', 'pointerdown', 'keydown'] as const

/**
 * Opens an hour grid on its busiest stretch — see openingMinutes — each time a new period is shown.
 *
 * It never takes the grid back: once the reader scrolls or presses in it, the period is left where
 * they put it, even if its events arrive afterwards. A period reached mid-drag is left alone too,
 * so the grid does not jump under the pointer.
 *
 * `period` names what is shown (the week's Monday, the day); `starts` are its events' start times.
 */
export function useOpeningScroll(period: string, starts: number[], dragging: boolean) {
  const scroller = useRef<HTMLDivElement | null>(null)
  const taken = useRef(false)
  const shownPeriod = useRef<string | null>(null)
  const startsKey = starts.join(',')

  useEffect(() => {
    const element = scroller.current
    if (!element) return
    const take = () => { taken.current = true }
    for (const type of READER_INPUT) element.addEventListener(type, take, { passive: true })
    return () => { for (const type of READER_INPUT) element.removeEventListener(type, take) }
  }, [])

  useEffect(() => {
    if (shownPeriod.current !== period) { shownPeriod.current = period; taken.current = false }
    if (dragging) taken.current = true
    const element = scroller.current
    if (taken.current || !element) return
    const visibleMinutes = (element.clientHeight / HOUR_HEIGHT) * 60
    const minutes = openingMinutes(startsKey === '' ? [] : startsKey.split(',').map(Number), visibleMinutes)
    element.scrollTop = (minutes / 60) * HOUR_HEIGHT
  }, [period, startsKey, dragging])

  return scroller
}
