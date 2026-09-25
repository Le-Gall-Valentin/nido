import type { ContributionInput } from '@/entities/finance'

/** Rounds to the nearest cent, expressed as an integer, so two amounts can be compared exactly. */
function toCents(amount: number): number {
  return Math.round(amount * 100)
}

/**
 * Builds the contributor payload sent to the API from what the transaction
 * form currently holds. `customShares` null means the user left the split
 * on "equal" — every contributor is submitted with a null shareAmount so
 * the backend computes the equal split itself. Non-null means the user
 * customized at least one share — every selected contributor must then
 * have an explicit amount, and they must sum to the total exactly to the
 * cent (comparing rounded cents absorbs float representation noise without
 * being any looser than that — matching the backend's own BigDecimal
 * comparison after rescaling both sides to 2 decimals), otherwise this
 * returns null so the form can show an error instead of submitting an
 * invalid split.
 */
export function resolveContributionShares(
  amount: number, contributorIds: string[], customShares: Record<string, number> | null
): ContributionInput[] | null {
  if (contributorIds.length === 0) {
    return []
  }
  if (customShares === null) {
    return contributorIds.map((memberId) => ({ memberId, shareAmount: null }))
  }
  // Built once and narrowed here rather than checked with .some() and read again: a missing share
  // and the sum are two questions about the same list, and looking it up twice is what let the
  // second read be typed as possibly undefined while the first had already ruled that out.
  const contributions: ContributionInput[] = []
  let sum = 0
  for (const memberId of contributorIds) {
    const shareAmount = customShares[memberId]
    if (shareAmount === undefined) {
      return null
    }
    sum += shareAmount
    contributions.push({ memberId, shareAmount })
  }
  if (toCents(sum) !== toCents(amount)) {
    return null
  }
  return contributions
}
