import { describe, expect, it } from 'vitest'
import { resolveContributionShares } from './resolveContributionShares'

describe('resolveContributionShares', () => {
  it('returns an empty array when there are no contributors', () => {
    expect(resolveContributionShares(50, [], null)).toEqual([])
  });

  it('requests an equal split (null shareAmount) when no custom shares are given', () => {
    const result = resolveContributionShares(50, ['alice', 'bob'], null)

    expect(result).toEqual([
      { memberId: 'alice', shareAmount: null },
      { memberId: 'bob', shareAmount: null },
    ])
  })

  it('returns the custom shares when they sum to the total amount', () => {
    const result = resolveContributionShares(100, ['alice', 'bob'], { alice: 70, bob: 30 })

    expect(result).toEqual([
      { memberId: 'alice', shareAmount: 70 },
      { memberId: 'bob', shareAmount: 30 },
    ])
  })

  it('tolerates a sub-cent rounding difference', () => {
    const result = resolveContributionShares(10, ['alice', 'bob', 'carl'], { alice: 3.34, bob: 3.33, carl: 3.33 })

    expect(result).not.toBeNull()
  })

  it('returns null when the custom shares do not sum to the total amount', () => {
    const result = resolveContributionShares(100, ['alice', 'bob'], { alice: 70, bob: 20 })

    expect(result).toBeNull()
  })

  it('rejects a half-a-cent-or-more discrepancy, matching the backend\'s exact-to-the-cent comparison', () => {
    // A looser float epsilon would have silently accepted this; the backend compares
    // BigDecimal amounts rescaled to 2 decimals exactly, so this must be rejected too.
    const result = resolveContributionShares(100, ['alice', 'bob'], { alice: 70.005, bob: 30 })

    expect(result).toBeNull()
  })

  it('returns null when a contributor is missing from the custom shares', () => {
    const result = resolveContributionShares(100, ['alice', 'bob'], { alice: 100 })

    expect(result).toBeNull()
  })
})
