import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Dialog, Button, Input, CTA_BUTTON_STYLE } from '@/shared/ui'
import type { SpaceMember } from '@/entities/space'

// Same tolerance as resolveContributionShares.ts — absorbs float rounding so a
// contribution exactly equal to `remaining` isn't wrongly rejected as "too high".
const EPSILON = 0.005

export interface AddContributionInput {
  memberId: string
  amount: number
  date: string
}

interface AddContributionModalProps {
  goalName: string
  /** What's left to reach the goal's target (targetAmount - totalContributed). A contribution cannot exceed it. */
  remaining: number
  members: SpaceMember[]
  onSubmit: (input: AddContributionInput) => void
  onCancel: () => void
  isPending: boolean
  /** Set by the caller when the backend rejected the last submission. */
  submitError?: string | null
}

export function AddContributionModal({ goalName, remaining, members, onSubmit, onCancel, isPending, submitError = null }: AddContributionModalProps) {
  const { t } = useTranslation('finance')
  const [memberId, setMemberId] = useState(members[0]?.userId ?? '')
  const [amount, setAmount] = useState('')
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10))
  const [error, setError] = useState<string | null>(null)

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const numericAmount = Number(amount)
    if (!amount || Number.isNaN(numericAmount) || numericAmount <= 0) {
      setError(t('form.amount_required'))
      return
    }
    if (numericAmount - remaining > EPSILON) {
      setError(t('savings.contribution_too_high'))
      return
    }
    setError(null)
    onSubmit({ memberId, amount: numericAmount, date })
  }

  return (
    <Dialog open onClose={onCancel} title={t('savings.contribute_title', { name: goalName })}>
      <h3 className="mb-4 text-[19px] font-semibold text-fg-0">{t('savings.contribute_title', { name: goalName })}</h3>
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <label htmlFor="contribution-member" className="text-[13px] font-semibold text-fg-1">{t('savings.contributor_label')}</label>
          <select id="contribution-member" value={memberId} onChange={(e) => setMemberId(e.target.value)}
            className="rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3.5 py-[11px] text-[14.5px] text-fg-0 outline-none focus:border-accent">
            {members.map((m) => <option key={m.userId} value={m.userId}>{m.username ?? m.email}</option>)}
          </select>
        </div>
        <Input label={t('form.amount_label')} type="number" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} />
        <Input label={t('form.date_label')} type="date" value={date} onChange={(e) => setDate(e.target.value)} />

        {(error ?? submitError) && <p className="text-sm font-medium text-status-red">{error ?? submitError}</p>}

        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" onClick={onCancel}>{t('form.cancel')}</Button>
          <Button type="submit" isLoading={isPending} style={CTA_BUTTON_STYLE}>{t('form.save')}</Button>
        </div>
      </form>
    </Dialog>
  )
}
