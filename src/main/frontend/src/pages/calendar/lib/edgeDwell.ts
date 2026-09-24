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

export function horizontalZone(pointerX: number, rect: { left: number; right: number }): Zone {
  if (pointerX >= rect.right - EDGE_ZONE_PX) return 1
  if (pointerX <= rect.left + EDGE_ZONE_PX) return -1
  return 0
}

/** The phone week: scrolling comes first, so an edge only arms once there is nothing left to scroll. */
export function verticalZone(pointerY: number, viewport: { top: number; bottom: number },
                             canScrollUp: boolean, canScrollDown: boolean): Zone {
  if (pointerY >= viewport.bottom - EDGE_ZONE_PX && !canScrollDown) return 1
  if (pointerY <= viewport.top + EDGE_ZONE_PX && !canScrollUp) return -1
  return 0
}
