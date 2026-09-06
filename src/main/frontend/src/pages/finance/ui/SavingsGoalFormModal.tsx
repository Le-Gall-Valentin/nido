import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Dialog, Button, Input, CTA_BUTTON_STYLE } from '@/shared/ui'
import type { SavingsGoal } from '@/entities/finance'
import { SavingsGoalAppearancePicker } from './SavingsGoalAppearancePicker'
import { SAVINGS_GOAL_COLORS, SAVINGS_GOAL_GLYPHS } from '../lib/savingsGoalAppearance'

export interface SavingsGoalFormInput {
  name: string
  targetAmount: number
  targetDate: string | null
  color: string
  glyph: string
}

interface SavingsGoalFormModalProps {
  mode: 'create' | 'edit'
  goal?: SavingsGoal
  onSubmit: (input: SavingsGoalFormInput) => void
  onCancel: () => void
  /** Set by the caller when the backend rejected the last submission — distinct from the client-side checks below. */
  submitError?: string | null
}

export function SavingsGoalFormModal({ mode, goal, onSubmit, onCancel, submitError = null }: SavingsGoalFormModalProps) {
  const { t } = useTranslation('finance')
  const [name, setName] = useState(goal?.name ?? '')
  const [targetAmount, setTargetAmount] = useState(goal ? String(goal.targetAmount) : '')
  const [targetDate, setTargetDate] = useState(goal?.targetDate ?? '')
  const [color, setColor] = useState(goal?.color ?? SAVINGS_GOAL_COLORS[0])
  const [glyph, setGlyph] = useState(goal?.glyph ?? SAVINGS_GOAL_GLYPHS[0])
  const [error, setError] = useState<string | null>(null)

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!name.trim()) {
      setError(t('savings.name_required'))
      return
    }
    if (name.trim().length > 100) {
      setError(t('savings.name_too_long'))
      return
    }
    const amount = Number(targetAmount)
    if (!targetAmount || Number.isNaN(amount) || amount <= 0) {
      setError(t('savings.target_amount_required'))
      return
    }
    if (!/^\d+(\.\d{1,2})?$/.test(targetAmount.trim())) {
      setError(t('savings.target_amount_too_precise'))
      return
    }
    setError(null)
    onSubmit({ name: name.trim(), targetAmount: amount, targetDate: targetDate || null, color, glyph })
  }

  return (
    <Dialog open onClose={onCancel} title={mode === 'create' ? t('savings.create_title') : t('savings.edit_title')}>
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <Input label={t('savings.name_label')} value={name} onChange={(e) => setName(e.target.value)} />
        <Input label={t('savings.target_amount_label')} type="number" step="0.01" value={targetAmount} onChange={(e) => setTargetAmount(e.target.value)} />
        <Input label={t('savings.target_date_label')} type="date" value={targetDate} onChange={(e) => setTargetDate(e.target.value)} />
        <SavingsGoalAppearancePicker color={color} onColorChange={setColor} glyph={glyph} onGlyphChange={setGlyph} />

        {(error ?? submitError) && <p className="text-sm font-medium text-status-red">{error ?? submitError}</p>}

        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" onClick={onCancel}>{t('form.cancel')}</Button>
          <Button type="submit" style={CTA_BUTTON_STYLE}>{t('form.save')}</Button>
        </div>
      </form>
    </Dialog>
  )
}
