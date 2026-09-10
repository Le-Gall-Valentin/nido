import type { TransactionType } from '@/entities/finance'

interface ContributionLabelKeys {
  /** Who is holding the money: the one who paid on an expense, the one who received on an income. */
  holder: string
  holderRequired: string
  /** Who has a share in it: who owes one on an expense, who is owed one on an income. */
  parties: string
}

/**
 * An expense and an income are mirror images of each other. On an expense the payer fronts the
 * money and the others owe him their share; on an income the receiver holds money that belongs to
 * the others, so he owes them their share — the server folds the two in opposite directions. The
 * wording has to follow, otherwise a shared income reads as if somebody had paid for it.
 *
 * Kept in one place so the form, the validation message and the detail view can never drift apart.
 */
export function contributionLabelKeys(type: TransactionType): ContributionLabelKeys {
  return type === 'INCOME'
    ? { holder: 'form.receiver_label', holderRequired: 'form.receiver_required', parties: 'form.beneficiaries_label' }
    : { holder: 'form.payer_label', holderRequired: 'form.payer_required', parties: 'form.contributors_label' }
}
