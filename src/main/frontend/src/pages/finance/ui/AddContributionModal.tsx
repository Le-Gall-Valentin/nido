import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Dialog } from '@/shared/ui'
import type { SpaceMember } from '@/entities/space'

export interface AddContributionInput {
  memberId: string
  amount: number
  date: string
}

interface AddContributionModalProps {
  goalName: string
  members: SpaceMember[]
  onSubmit: (input: AddContributionInput) => void
  onCancel: () => void
  isPending: boolean
}

export function AddContributionModal({ goalName, members, onSubmit, onCancel, isPending }: AddContributionModalProps) {
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
    setError(null)
    onSubmit({ memberId, amount: numericAmount, date })
  }

  return (
    <Dialog open onClose={onCancel} title={t('savings.contribute_title', { name: goalName })}>
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && <p className="text-sm text-red-600">{error}</p>}
        <div>
          <label htmlFor="contribution-member" className="block text-sm font-medium">{t('savings.contributor_label')}</label>
          <select id="contribution-member" value={memberId} onChange={(e) => setMemberId(e.target.value)} className="mt-1 w-full rounded-md border px-3 py-2">
            {members.map((m) => <option key={m.userId} value={m.userId}>{m.username ?? m.email}</option>)}
          </select>
        </div>
        <div>
          <label htmlFor="contribution-amount" className="block text-sm font-medium">{t('form.amount_label')}</label>
          <input id="contribution-amount" type="number" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} className="mt-1 w-full rounded-md border px-3 py-2" />
        </div>
        <div>
          <label htmlFor="contribution-date" className="block text-sm font-medium">{t('form.date_label')}</label>
          <input id="contribution-date" type="date" value={date} onChange={(e) => setDate(e.target.value)} className="mt-1 w-full rounded-md border px-3 py-2" />
        </div>
        <div className="flex justify-end gap-2 pt-2">
          <button type="button" onClick={onCancel} className="rounded-md px-4 py-2 text-sm">{t('form.cancel')}</button>
          <button type="submit" disabled={isPending} className="rounded-md bg-fg-1 px-4 py-2 text-sm text-bg-1 disabled:opacity-50">{t('form.save')}</button>
        </div>
      </form>
    </Dialog>
  )
}
