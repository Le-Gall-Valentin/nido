/** Something drawn in an hour column, from `start` to `end` minutes past midnight. */
export interface Span<T> { item: T; start: number; end: number }

/**
 * Where a span sits across its column, as fractions of the column's width: `left` from the left
 * edge, `width` of it. `lane` also orders the drawing — a later lane is drawn over an earlier one.
 */
export interface Placed<T> { item: T; lane: number; lanes: number; left: number; width: number }

/** How far an event reaches into the lane after its own, in lanes — the overlap that shows the cascade. */
const REACH = 1.6

/**
 * Lays out the spans of one column so that overlapping events stay told apart, the way a paper
 * diary's second entry is written a little to the right of the first.
 *
 * Overlapping spans form a group; in each group every span takes the first lane free at its start
 * (earliest first, the longer first on a tie). Lane k starts k/n of the way across; each span
 * reaches a little into the next lane, and the last runs to the right edge. A span that overlaps
 * nothing keeps the whole column.
 */
export function layoutOverlaps<T>(spans: Span<T>[]): Placed<T>[] {
  const sorted = [...spans].sort((a, b) => a.start - b.start || b.end - a.end)
  const placed: Placed<T>[] = []
  let group: Array<{ span: Span<T>; lane: number }> = []
  let laneEnds: number[] = []
  let groupEnd = -Infinity

  const flush = () => {
    const lanes = laneEnds.length
    for (const { span, lane } of group) {
      const left = lane / lanes
      const width = lane === lanes - 1 ? 1 - left : Math.min(1 - left, REACH / lanes)
      placed.push({ item: span.item, lane, lanes, left, width })
    }
    group = []
    laneEnds = []
  }

  for (const span of sorted) {
    if (span.start >= groupEnd && group.length > 0) flush()
    let lane = laneEnds.findIndex((end) => end <= span.start)
    if (lane < 0) { lane = laneEnds.length; laneEnds.push(span.end) } else laneEnds[lane] = span.end
    group.push({ span, lane })
    groupEnd = group.length === 1 ? span.end : Math.max(groupEnd, span.end)
  }
  if (group.length > 0) flush()
  return placed
}
