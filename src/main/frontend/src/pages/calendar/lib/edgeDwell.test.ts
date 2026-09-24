import { describe, it, expect } from 'vitest'
import { IDLE, horizontalZone, stepDwell, verticalZone, type DwellState } from './edgeDwell'

function run(steps: Array<[zone: -1 | 0 | 1, now: number]>) {
  let state: DwellState = IDLE
  const fires: number[] = []
  for (const [zone, now] of steps) {
    const result = stepDwell(state, zone, now)
    state = result.state
    if (result.fire) fires.push(result.fire)
  }
  return fires
}

describe('stepDwell', () => {
  it('fires once the pointer has held the edge for 600ms', () => {
    expect(run([[1, 0], [1, 599]])).toEqual([])
    expect(run([[1, 0], [1, 600]])).toEqual([1])
  })

  it('repeats every 900ms while held, so several periods can be skipped', () => {
    expect(run([[1, 0], [1, 600], [1, 1499], [1, 1500], [1, 2400]])).toEqual([1, 1, 1])
  })

  it('starts over when the pointer leaves the edge', () => {
    expect(run([[1, 0], [0, 400], [1, 500], [1, 1000]])).toEqual([])
  })

  it('starts over when the pointer jumps to the other edge', () => {
    expect(run([[1, 0], [-1, 500], [-1, 1099], [-1, 1100]])).toEqual([-1])
  })
})

describe('horizontalZone', () => {
  const rect = { left: 100, right: 900 }
  it('arms within 32px of an edge and beyond it', () => {
    expect(horizontalZone(880, rect)).toBe(1)
    expect(horizontalZone(1200, rect)).toBe(1)
    expect(horizontalZone(120, rect)).toBe(-1)
    expect(horizontalZone(40, rect)).toBe(-1)
    expect(horizontalZone(500, rect)).toBe(0)
  })
})

describe('verticalZone', () => {
  const viewport = { top: 0, bottom: 800 }
  it('arms at the bottom only once there is nothing left to scroll down', () => {
    expect(verticalZone(790, viewport, true, true)).toBe(0)
    expect(verticalZone(790, viewport, true, false)).toBe(1)
  })

  it('arms at the top only once there is nothing left to scroll up', () => {
    expect(verticalZone(10, viewport, true, true)).toBe(0)
    expect(verticalZone(10, viewport, false, true)).toBe(-1)
  })
})
