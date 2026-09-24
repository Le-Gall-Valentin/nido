/** Where an empty week or day opens — the start of a working day rather than midnight. */
export const DEFAULT_OPENING_MINUTES = 8 * 60
/** Room left above the first event of the stretch, so it does not open flush against the top. */
const LEAD_MINUTES = 30

/**
 * The time the hour grid opens on: the stretch one screen tall that holds the most event starts,
 * the earliest of equals, opened half an hour before its first event. Few events begin at midnight,
 * where the grid used to open.
 *
 * `starts` are the minutes past midnight at which the events shown begin — only true starts, so
 * the middle pieces of an event lasting several days do not pull every week back to midnight.
 */
export function openingMinutes(starts: number[], visibleMinutes: number): number {
  if (starts.length === 0) return DEFAULT_OPENING_MINUTES
  const sorted = [...starts].sort((a, b) => a - b)
  let best = sorted[0] ?? 0
  let bestCount = 0
  for (const from of sorted) {
    const count = sorted.filter((start) => start >= from && start < from + Math.max(visibleMinutes, 1)).length
    if (count > bestCount) { best = from; bestCount = count }
  }
  return Math.max(0, best - LEAD_MINUTES)
}
