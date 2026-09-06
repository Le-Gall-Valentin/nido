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
  const raw = shareAmounts.map((amount) => (amount / total) * 100)
  const floors = raw.map(Math.floor)
  const leftover = Math.round(100 - floors.reduce((sum, f) => sum + f, 0))
  const order = floors.map((_, i) => i).sort((a, b) => (raw[b] - floors[b]) - (raw[a] - floors[a]))
  const result = [...floors]
  for (let i = 0; i < leftover; i++) {
    result[order[i]] += 1
  }
  return result
}
