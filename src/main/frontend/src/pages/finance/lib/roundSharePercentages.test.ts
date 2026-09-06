import { describe, expect, it } from 'vitest'
import { roundSharePercentages } from './roundSharePercentages'

describe('roundSharePercentages', () => {
  it('sums to exactly 100 even when naive rounding would not', () => {
    // 10/30, 10/30, 10/30 = 33.33%, 33.33%, 33.33% — naive Math.round gives 33+33+33=99.
    const result = roundSharePercentages([10, 10, 10], 30)

    expect(result.reduce((a, b) => a + b, 0)).toBe(100)
  })

  it('gives the leftover point to the share(s) with the largest fractional remainder', () => {
    const result = roundSharePercentages([10, 10, 10], 30)

    expect(result.filter((p) => p === 34)).toHaveLength(1)
    expect(result.filter((p) => p === 33)).toHaveLength(2)
  })

  it('matches naive rounding when the shares already divide evenly', () => {
    const result = roundSharePercentages([15, 15], 30)

    expect(result).toEqual([50, 50])
  })

  it('returns all zeros when the total is zero', () => {
    expect(roundSharePercentages([10, 20], 0)).toEqual([0, 0])
  })

  it('returns an empty array for no contributors', () => {
    expect(roundSharePercentages([], 100)).toEqual([])
  })
})
