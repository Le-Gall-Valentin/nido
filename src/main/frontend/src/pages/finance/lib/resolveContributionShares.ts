import type { ContributionInput } from '@/entities/finance'

const EPSILON = 0.005

/**
 * Builds the contributor payload sent to the API from what the transaction
 * form currently holds. `customShares` null means the user left the split
 * on "equal" — every contributor is submitted with a null shareAmount so
 * the backend computes the equal split itself. Non-null means the user
 * customized at least one share — every selected contributor must then
 * have an explicit amount, and they must sum to the total (a small
 * tolerance absorbs float rounding), otherwise this returns null so the
 * form can show an error instead of submitting an invalid split.
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
  const shares = contributorIds.map((memberId) => customShares[memberId])
  if (shares.some((share) => share === undefined)) {
    return null
  }
  const sum = shares.reduce((total, share) => total + share, 0)
  if (Math.abs(sum - amount) > EPSILON) {
    return null
  }
  return contributorIds.map((memberId) => ({ memberId, shareAmount: customShares[memberId] }))
}
