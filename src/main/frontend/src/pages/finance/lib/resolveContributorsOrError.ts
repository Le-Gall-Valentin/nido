import type { ContributionInput, TransactionType } from '@/entities/finance'
import { contributionLabelKeys } from './contributionLabelKeys'
import { resolveContributionShares } from './resolveContributionShares'

export type ContributorsResolution = { contributors: ContributionInput[]; error: null } | { contributors: null; error: string }

/**
 * The payer/contributors/shares half of transaction and recurring-series form validation,
 * shared by TransactionFormModal and RecurringSeriesFormModal since both need exactly this.
 */
export function resolveContributorsOrError(
  amount: number, type: TransactionType, canPickContributors: boolean, payerId: string, contributorIds: string[],
  customizeShares: boolean, customShares: Record<string, number>,
  t: (key: string) => string
): ContributorsResolution {
  if (canPickContributors && !payerId) {
    return { contributors: null, error: t(contributionLabelKeys(type).holderRequired) }
  }
  const resolved = resolveContributionShares(amount, contributorIds, customizeShares ? customShares : null)
  if (resolved === null) {
    return { contributors: null, error: t('form.shares_invalid') }
  }
  return { contributors: resolved, error: null }
}
