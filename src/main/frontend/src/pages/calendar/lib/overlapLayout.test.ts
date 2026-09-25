import { describe, it, expect } from 'vitest'
import { layoutOverlaps } from './overlapLayout'

const h = (hhmm: string) => { const [a, b] = hhmm.split(':').map(Number); return a * 60 + b }
const span = (id: string, from: string, to: string) => ({ item: id, start: h(from), end: h(to) })

/** "id lane/lanes left width", widths rounded to 3 places. */
function laidOut(spans: ReturnType<typeof span>[]) {
  return layoutOverlaps(spans).map((p) => `${p.item} ${p.lane}/${p.lanes} ${+p.left.toFixed(3)} ${+p.width.toFixed(3)}`).sort()
}

describe('layoutOverlaps', () => {
  it('gives an event that overlaps nothing the whole column', () => {
    expect(laidOut([span('a', '09:00', '10:00')])).toEqual(['a 0/1 0 1'])
  })

  it('does not count two events that only touch as overlapping', () => {
    expect(laidOut([span('a', '09:00', '10:00'), span('b', '10:00', '11:00')])).toEqual(['a 0/1 0 1', 'b 0/1 0 1'])
  })

  it('offsets the second of two overlapping events, which overlaps the first a little', () => {
    // The first keeps its left edge and some of the second lane; the second runs to the right edge.
    expect(laidOut([span('a', '09:00', '11:00'), span('b', '10:00', '12:00')])).toEqual(['a 0/2 0 0.8', 'b 1/2 0.5 0.5'])
  })

  it('cascades three events that all overlap', () => {
    expect(laidOut([span('a', '09:00', '12:00'), span('b', '09:30', '12:00'), span('c', '10:00', '12:00')]))
      .toEqual(['a 0/3 0 0.533', 'b 1/3 0.333 0.533', 'c 2/3 0.667 0.333'])
  })

  it('puts an event back in the first lane free at its start', () => {
    // c starts after a ended, so it needs no third lane.
    expect(laidOut([span('a', '09:00', '10:00'), span('b', '09:30', '10:30'), span('c', '10:15', '11:00')]))
      .toEqual(['a 0/2 0 0.8', 'b 1/2 0.5 0.5', 'c 0/2 0 0.8'])
  })

  it('lays out each group of overlaps on its own — a later lone event gets the whole column', () => {
    expect(laidOut([span('a', '09:00', '10:00'), span('b', '09:30', '10:30'), span('c', '12:00', '13:00')]))
      .toEqual(['a 0/2 0 0.8', 'b 1/2 0.5 0.5', 'c 0/1 0 1'])
  })

  it('gives the first lane to the longer of two events starting together', () => {
    expect(laidOut([span('short', '09:00', '09:30'), span('long', '09:00', '12:00')]))
      .toEqual(['long 0/2 0 0.8', 'short 1/2 0.5 0.5'])
  })
})
