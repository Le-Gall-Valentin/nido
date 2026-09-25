import type { DragIntent } from './dragTypes'

/** How close to the top or bottom of the phone list a finger arms the vertical edge. */
export const EDGE_ZONE_PX = 32
export const FIRST_DELAY_MS = 600
export const REPEAT_MS = 900

export type Zone = -1 | 0 | 1
export interface DwellState { zone: Zone; since: number | null; fired: boolean }
export const IDLE: DwellState = { zone: 0, since: null, fired: false }

/**
 * One step of the edge countdown: idle → armed → fired → rearmed. Pure, so the timing can be tested
 * without a browser; the hook only feeds it the zone and the clock.
 */
export function stepDwell(state: DwellState, zone: Zone, now: number): { state: DwellState; fire: Zone } {
  if (zone === 0) return { state: IDLE, fire: 0 }
  if (zone !== state.zone || state.since === null) return { state: { zone, since: now, fired: false }, fire: 0 }
  const wait = state.fired ? REPEAT_MS : FIRST_DELAY_MS
  if (now - state.since >= wait) return { state: { zone, since: now, fired: true }, fire: zone }
  return { state, fire: 0 }
}

/**
 * Past a side of the calendar. Only past it: the reader asked to push out of the calendar, and a
 * zone inside it would page the week under anyone taking a moment to aim at a Sunday.
 */
export function horizontalZone(pointerX: number, rect: { left: number; right: number }): Zone {
  if (pointerX >= rect.right) return 1
  if (pointerX <= rect.left) return -1
  return 0
}

/** The phone week: scrolling comes first, so an edge only arms once there is nothing left to scroll. */
export function verticalZone(pointerY: number, viewport: { top: number; bottom: number },
                             canScrollUp: boolean, canScrollDown: boolean): Zone {
  if (pointerY >= viewport.bottom - EDGE_ZONE_PX && !canScrollDown) return 1
  if (pointerY <= viewport.top + EDGE_ZONE_PX && !canScrollUp) return -1
  return 0
}

export interface EdgeLayout {
  /** Whether the desktop layout is on screen. */
  wide: boolean
  /** The calendar's own box. */
  container: { left: number; right: number } | null
  /** The phone list's scroller: its visible box, and whether it can still scroll either way. */
  scroller: { top: number; bottom: number; canScrollUp: boolean; canScrollDown: boolean } | null
}

/** Which edge a drag is holding, if any — the one place that decides which drags page at all. */
export function dragZone(intent: DragIntent, pointer: { x: number; y: number }, layout: EdgeLayout): Zone {
  // A resize stretches an event within its own day; paging under it would read the pointer
  // against another period's column.
  if (intent.kind !== 'move') return 0
  if (intent.from === 'row') {
    const { scroller } = layout
    return scroller ? verticalZone(pointer.y, scroller, scroller.canScrollUp, scroller.canScrollDown) : 0
  }
  // The phone day view only changes time; a finger near the side of a narrow screen must not flip days.
  if (!layout.wide || !layout.container) return 0
  return horizontalZone(pointer.x, layout.container)
}
