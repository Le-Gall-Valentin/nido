import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Dialog, Button, Input, CTA_BUTTON_STYLE } from '@/shared/ui'
import type { SpaceMember } from '@/entities/space'
import type { Category, ContributionInput, RecurrenceInput, Transaction, TransactionType } from '@/entities/finance'
import { resolveContributorsOrError } from '../lib/resolveContributorsOrError'
import { ContributorsPicker } from './ContributorsPicker'

export interface TransactionFormInput {
  label: string
  amount: number
  type: TransactionType
  categoryId: string
  date: string
  payerId: string | null
  contributors: ContributionInput[]
  recurrence: RecurrenceInput | null
}

interface TransactionFormModalProps {
  mode: 'create' | 'edit'
  transaction?: Transaction
  categories: Category[]
  members: SpaceMember[]
  /** False in a PERSONAL space — payer/contributors/recurrence-rotation concepts don't apply there. */
  canPickContributors: boolean
  /** The logged-in user's id — pre-selects them as the payer for a new shared transaction, since that's who usually fills out this form. */
  currentUserId?: string | null
  onSubmit: (input: TransactionFormInput) => void
  onCancel: () => void
  /** Set by the caller when the backend rejected the last submission (e.g. a validation error) — distinct from the client-side checks below. */
  submitError?: string | null
}

const SELECT_CLASSNAME = 'rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3.5 py-[11px] text-[14.5px] text-fg-0 outline-none focus:border-accent'

export function TransactionFormModal({ mode, transaction, categories, members, canPickContributors, currentUserId = null, onSubmit, onCancel, submitError = null }: TransactionFormModalProps) {
  const { t } = useTranslation('finance')
  const [label, setLabel] = useState(transaction?.label ?? '')
  const [amount, setAmount] = useState(transaction ? String(transaction.amount) : '')
  const [type, setType] = useState<TransactionType>(transaction?.type ?? 'EXPENSE')
  const [categoryId, setCategoryId] = useState(transaction?.categoryId ?? categories[0]?.id ?? '')
  const [date, setDate] = useState(transaction?.date ?? new Date().toISOString().slice(0, 10))
  const [payerId, setPayerId] = useState<string>(transaction?.payerId ?? currentUserId ?? '')
  const [contributorIds, setContributorIds] = useState<string[]>(
    transaction?.contributors.map((c) => c.memberId) ?? (canPickContributors ? members.map((m) => m.userId) : [])
  )
  const [customizeShares, setCustomizeShares] = useState(false)
  const [customShares, setCustomShares] = useState<Record<string, number>>({})
  const [recurring, setRecurring] = useState(false)
  const [intervalType, setIntervalType] = useState<'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY'>('MONTHLY')
  const [intervalCount, setIntervalCount] = useState('1')
  const [endDate, setEndDate] = useState('')
  const [error, setError] = useState<string | null>(null)

  function toggleContributor(memberId: string) {
    setContributorIds((ids) => (ids.includes(memberId) ? ids.filter((id) => id !== memberId) : [...ids, memberId]))
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!label.trim()) {
      setError(t('form.label_required'))
      return
    }
    const numericAmount = Number(amount)
    if (!amount || Number.isNaN(numericAmount) || numericAmount <= 0) {
      setError(t('form.amount_required'))
      return
    }
    if (!categoryId) {
      setError(t('form.category_required'))
      return
    }
    const resolution = resolveContributorsOrError(
      numericAmount, canPickContributors, payerId, contributorIds, customizeShares, customShares, t)
    if (resolution.error !== null) {
      setError(resolution.error)
      return
    }
    if (recurring && endDate && endDate < date) {
      setError(t('recurring_series.end_date_before_start'))
      return
    }
    setError(null)
    onSubmit({
      label: label.trim(), amount: numericAmount, type, categoryId, date, payerId: payerId || null,
      contributors: resolution.contributors,
      recurrence: recurring ? { intervalType, intervalCount: Number(intervalCount), anchorDate: date, endDate: endDate || null } : null,
    })
  }

  return (
    <Dialog open onClose={onCancel} title={mode === 'create' ? t('form.create_title') : t('form.edit_title')} maxWidth="max-w-lg">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <div className="flex gap-1 rounded-[11px] bg-bg-2 p-1">
          <button type="button" onClick={() => setType('EXPENSE')} aria-pressed={type === 'EXPENSE'}
            className={`flex-1 rounded-[8px] px-3 py-2 text-sm font-semibold transition-colors ${type === 'EXPENSE' ? 'bg-bg-1 text-fg-0 shadow-sm' : 'text-fg-3'}`}>
            {t('type.EXPENSE')}
          </button>
          <button type="button" onClick={() => setType('INCOME')} aria-pressed={type === 'INCOME'}
            className={`flex-1 rounded-[8px] px-3 py-2 text-sm font-semibold transition-colors ${type === 'INCOME' ? 'bg-bg-1 text-fg-0 shadow-sm' : 'text-fg-3'}`}>
            {t('type.INCOME')}
          </button>
        </div>

        <Input label={t('form.label_label')} value={label} onChange={(e) => setLabel(e.target.value)} />
        <Input label={t('form.amount_label')} type="number" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} />

        <div className="flex flex-col gap-1.5">
          <label htmlFor="finance-category" className="text-[13px] font-semibold text-fg-1">{t('form.category_label')}</label>
          <select id="finance-category" value={categoryId} onChange={(e) => setCategoryId(e.target.value)} className={SELECT_CLASSNAME}>
            {categories.map((c) => <option key={c.id} value={c.id}>{c.label}</option>)}
          </select>
        </div>

        <Input label={t('form.date_label')} type="date" value={date} onChange={(e) => setDate(e.target.value)} />

        {canPickContributors && (
          <ContributorsPicker
            members={members} payerId={payerId} onPayerChange={setPayerId}
            contributorIds={contributorIds} onToggleContributor={toggleContributor}
            customizeShares={customizeShares} onCustomizeSharesChange={setCustomizeShares}
            customShares={customShares}
            onCustomShareChange={(id, value) => setCustomShares((shares) => ({ ...shares, [id]: value }))}
          />
        )}

        {mode === 'create' && (
          <>
            <label className="flex items-center gap-2 text-sm font-semibold text-fg-1">
              <input type="checkbox" checked={recurring} onChange={(e) => setRecurring(e.target.checked)} />
              {t('form.recurring_label')}
            </label>
            {recurring && (
              <div className="flex flex-col gap-3 rounded-[10px] bg-bg-2 p-3">
                <div className="flex items-end gap-2">
                  <Input label={t('form.recurrence_interval_count_label')} type="number" min={1}
                    value={intervalCount} className="w-20"
                    onChange={(e) => setIntervalCount(e.target.value)} />
                  <select
                    aria-label={t('form.recurrence_interval_type_label')}
                    value={intervalType} onChange={(e) => setIntervalType(e.target.value as 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY')}
                    className={SELECT_CLASSNAME}>
                    <option value="DAILY">{t('form.interval.DAILY')}</option>
                    <option value="WEEKLY">{t('form.interval.WEEKLY')}</option>
                    <option value="MONTHLY">{t('form.interval.MONTHLY')}</option>
                    <option value="YEARLY">{t('form.interval.YEARLY')}</option>
                  </select>
                </div>
                <Input label={t('recurring_series.end_date_label')} type="date" value={endDate}
                  onChange={(e) => setEndDate(e.target.value)} />
              </div>
            )}
          </>
        )}

        {(error ?? submitError) && <p className="text-sm font-medium text-status-red">{error ?? submitError}</p>}

        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" onClick={onCancel}>{t('form.cancel')}</Button>
          <Button type="submit" style={CTA_BUTTON_STYLE}>{t('form.save')}</Button>
        </div>
      </form>
    </Dialog>
  )
}
