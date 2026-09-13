/**
 * Rounds each contributor's share of `total` to a whole percentage, guaranteeing the
 * results sum to exactly 100 — independently rounding each one (plain Math.round) can
 * land a cent or two short (e.g. three equal thirds each rounding down to 33%, summing
 * to 99%). Uses the largest-remainder method: floor every share, then hand the leftover
 * points to the shares with the largest fractional part first.
 */
export function roundSharePercentages(shareAmounts: number[], total: number): number[] {
  if (total <= 0 || shareAmounts.length === 0) {
    return shareAmounts.map(() => 0)
  }
  // Paired rather than indexed in parallel: the comparator below needs each share's fractional part
  // next to its position, and reading two arrays by index left the compiler unable to see that the
  // positions came from the arrays themselves.
  const parts = shareAmounts.map((amount, index) => {
    const exact = (amount / total) * 100
    const floor = Math.floor(exact)
    return { index, floor, remainder: exact - floor }
  })
  const result = parts.map((part) => part.floor)

  // Bounded by the number of shares, which it was not: when the shares do not add up to the total —
  // a partial split the caller can legitimately hold — leftover exceeds the array and the extra
  // rounds were writing past its end. Nothing visible came of it, because assigning to a missing
  // index hangs a property off the array rather than growing it, but the loop was running on
  // nothing. What it should do in that case is hand out the points it has and stop.
  const floorSum = result.reduce((sum, floor) => sum + floor, 0)
  const leftover = Math.min(Math.round(100 - floorSum), result.length)
  const byLargestRemainder = [...parts].sort((a, b) => b.remainder - a.remainder)
  for (const part of byLargestRemainder.slice(0, leftover)) {
    result[part.index] = (result[part.index] ?? part.floor) + 1
  }
  return result
}
