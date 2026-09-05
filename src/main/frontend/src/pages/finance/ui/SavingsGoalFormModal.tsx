import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Dialog, Button, Input, CTA_BUTTON_STYLE } from '@/shared/ui'
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
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <Input label={t('savings.name_label')} value={name} onChange={(e) => setName(e.target.value)} />
        <Input label={t('savings.target_amount_label')} type="number" step="0.01" value={targetAmount} onChange={(e) => setTargetAmount(e.target.value)} />
        <Input label={t('savings.target_date_label')} type="date" value={targetDate} onChange={(e) => setTargetDate(e.target.value)} />

        {error && <p className="text-sm font-medium text-status-red">{error}</p>}

        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" onClick={onCancel}>{t('form.cancel')}</Button>
          <Button type="submit" style={CTA_BUTTON_STYLE}>{t('form.save')}</Button>
        </div>
      </form>
    </Dialog>
  )
}
