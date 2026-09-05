import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Dialog } from '@/shared/ui'
import type { SavingsGoal } from '@/entities/finance'

export interface SavingsGoalFormInput {
  name: string
  targetAmount: number
  targetDate: string | null
}

interface SavingsGoalFormModalProps {
  mode: 'create' | 'edit'
  goal?: SavingsGoal
  onSubmit: (input: SavingsGoalFormInput) => void
  onCancel: () => void
}

export function SavingsGoalFormModal({ mode, goal, onSubmit, onCancel }: SavingsGoalFormModalProps) {
  const { t } = useTranslation('finance')
  const [name, setName] = useState(goal?.name ?? '')
  const [targetAmount, setTargetAmount] = useState(goal ? String(goal.targetAmount) : '')
  const [targetDate, setTargetDate] = useState(goal?.targetDate ?? '')
  const [error, setError] = useState<string | null>(null)

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!name.trim()) {
      setError(t('savings.name_required'))
      return
    }
    const amount = Number(targetAmount)
    if (!targetAmount || Number.isNaN(amount) || amount <= 0) {
      setError(t('savings.target_amount_required'))
      return
    }
    setError(null)
    onSubmit({ name: name.trim(), targetAmount: amount, targetDate: targetDate || null })
  }

  return (
    <Dialog open onClose={onCancel} title={mode === 'create' ? t('savings.create_title') : t('savings.edit_title')}>
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && <p className="text-sm text-red-600">{error}</p>}
        <div>
          <label htmlFor="savings-name" className="block text-sm font-medium">{t('savings.name_label')}</label>
          <input id="savings-name" value={name} onChange={(e) => setName(e.target.value)} className="mt-1 w-full rounded-md border px-3 py-2" />
        </div>
        <div>
          <label htmlFor="savings-target" className="block text-sm font-medium">{t('savings.target_amount_label')}</label>
          <input id="savings-target" type="number" step="0.01" value={targetAmount} onChange={(e) => setTargetAmount(e.target.value)} className="mt-1 w-full rounded-md border px-3 py-2" />
        </div>
        <div>
          <label htmlFor="savings-date" className="block text-sm font-medium">{t('savings.target_date_label')}</label>
          <input id="savings-date" type="date" value={targetDate} onChange={(e) => setTargetDate(e.target.value)} className="mt-1 w-full rounded-md border px-3 py-2" />
        </div>
        <div className="flex justify-end gap-2 pt-2">
          <button type="button" onClick={onCancel} className="rounded-md px-4 py-2 text-sm">{t('form.cancel')}</button>
          <button type="submit" className="rounded-md bg-fg-1 px-4 py-2 text-sm text-bg-1">{t('form.save')}</button>
        </div>
      </form>
    </Dialog>
  )
}
