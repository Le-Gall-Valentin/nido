import { useCallback, useEffect, useRef, useState } from 'react'
import { sameSchedule } from '../lib/dragResolution'
import type { ScheduleChange } from '@/entities/calendar'
import { bandRange, selectionRange, type GridPoint } from '../lib/gridSelection'
import { exactMinutesAt } from '../lib/timeMath'

type Picking =
  | { kind: 'hours'; anchor: GridPoint; current: GridPoint }
  | { kind: 'band'; anchor: string; current: string }

export interface PickedOut { kind: Picking['kind']; range: ScheduleChange }

function rangeOf(picking: Picking): ScheduleChange {
  return picking.kind === 'hours'
    ? selectionRange(picking.anchor, picking.current)
    : bandRange(picking.anchor, picking.current)
}

/** The column or band the pointer is over now — a drag can cross into another day's. */
function under(x: number, y: number, testId: string): HTMLElement | null {
  const hits = typeof document.elementsFromPoint === 'function' ? document.elementsFromPoint(x, y) : []
  const hit = hits.find((element) => element instanceof HTMLElement && element.dataset.testid === testId)
  return (hit as HTMLElement | undefined) ?? null
}

/**
 * A mouse press on empty grid. Pressing an event or a resize handle belongs to the drag, and a
 * finger to the scroller and the long press.
 */
function accepts(event: React.PointerEvent): boolean {
  if (event.pointerType !== 'mouse' || event.button !== 0) return false
  return !(event.target as Element).closest('button, [data-testid^="resize-"]')
}

/**
 * Picking out a time in the week or day grid — press on an empty slot, drag, release — to add an
 * event there. Inert without `onPick`, which is how a viewer gets nothing.
 */
export function useGridSelection(onPick: ((range: ScheduleChange) => void) | undefined) {
  const [shown, setShown] = useState<PickedOut | null>(null)
  const onPickRef = useRef(onPick)
  useEffect(() => { onPickRef.current = onPick }, [onPick])
  const stop = useRef<(() => void) | null>(null)
  useEffect(() => () => stop.current?.(), [])

  const follow = useCallback((first: Picking, next: (event: PointerEvent) => Picking) => {
    stop.current?.()
    let latest = first
    // Redrawn only when the picked-out time changes, not on every pixel the pointer moves.
    const show = (picking: Picking) => setShown((current) => {
      const range = rangeOf(picking)
      return current && current.kind === picking.kind && sameSchedule(current.range, range) ? current : { kind: picking.kind, range }
    })
    const onMove = (event: PointerEvent) => { latest = next(event); show(latest) }
    const onUp = () => { end(); onPickRef.current?.(rangeOf(latest)) }
    const onKey = (event: KeyboardEvent) => { if (event.key === 'Escape') end() }
    // Given up like Escape, never picked: a release that happens in another window never comes
    // back, and the next click here would create an event over a time nobody chose.
    const onLost = () => end()
    function end() {
      window.removeEventListener('pointermove', onMove)
      window.removeEventListener('pointerup', onUp)
      window.removeEventListener('keydown', onKey)
      window.removeEventListener('pointercancel', onLost)
      window.removeEventListener('blur', onLost)
      stop.current = null
      setShown(null)
    }
    window.addEventListener('pointermove', onMove)
    window.addEventListener('pointerup', onUp)
    window.addEventListener('keydown', onKey)
    window.addEventListener('pointercancel', onLost)
    window.addEventListener('blur', onLost)
    stop.current = end
    show(first)
  }, [])

  const startHours = useCallback((event: React.PointerEvent<HTMLElement>, day: string) => {
    if (!onPickRef.current || !accepts(event)) return
    // Keeps the browser from selecting the hour labels while the drag crosses them.
    event.preventDefault()
    const own = event.currentTarget
    const anchor = { day, minutes: exactMinutesAt(event.clientY, own.getBoundingClientRect().top) }
    follow({ kind: 'hours', anchor, current: anchor }, (move) => {
      const column = under(move.clientX, move.clientY, 'hour-column') ?? own
      return { kind: 'hours', anchor, current: {
        day: column.dataset.day ?? day, minutes: exactMinutesAt(move.clientY, column.getBoundingClientRect().top),
      } }
    })
  }, [follow])

  const startBand = useCallback((event: React.PointerEvent<HTMLElement>, day: string) => {
    if (!onPickRef.current || !accepts(event)) return
    event.preventDefault()
    follow({ kind: 'band', anchor: day, current: day }, (move) => {
      const band = under(move.clientX, move.clientY, 'all-day-band')
      return { kind: 'band', anchor: day, current: band?.dataset.day ?? day }
    })
  }, [follow])

  return { shown, startHours, startBand }
}
