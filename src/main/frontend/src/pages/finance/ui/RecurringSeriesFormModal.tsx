import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Dialog, Button, Input, CTA_BUTTON_STYLE } from '@/shared/ui'
import type { SpaceMember } from '@/entities/space'
import type { Category, ContributionInput, RecurrenceInput, RecurrenceInterval, RecurringSeries, TransactionType } from '@/entities/finance'
import { resolveContributorsOrError } from '../lib/resolveContributorsOrError'
import { ContributorsPicker } from './ContributorsPicker'

export interface RecurringSeriesFormInput {
  label: string
  amount: number
  type: TransactionType
  categoryId: string
  payerId: string | null
  contributors: ContributionInput[]
  recurrence: RecurrenceInput
}

interface RecurringSeriesFormModalProps {
  series: RecurringSeries
  categories: Category[]
  members: SpaceMember[]
  canPickContributors: boolean
  onSubmit: (input: RecurringSeriesFormInput) => void
  onCancel: () => void
  /** Set by the caller when the backend rejected the last submission — distinct from the client-side checks below. */
  submitError?: string | null
}

const SELECT_CLASSNAME = 'rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3.5 py-[11px] text-[14.5px] text-fg-0 outline-none focus:border-accent'

export function RecurringSeriesFormModal({ series, categories, members, canPickContributors, onSubmit, onCancel, submitError = null }: RecurringSeriesFormModalProps) {
  const { t } = useTranslation('finance')
  const [label, setLabel] = useState(series.label)
  const [amount, setAmount] = useState(String(series.amount))
  const [type, setType] = useState<TransactionType>(series.type)
  const [categoryId, setCategoryId] = useState(series.categoryId)
  const [payerId, setPayerId] = useState<string>(series.payerId ?? '')
  const [contributorIds, setContributorIds] = useState<string[]>(series.contributors.map((c) => c.memberId))
  const [customizeShares, setCustomizeShares] = useState(false)
  const [customShares, setCustomShares] = useState<Record<string, number>>(
    Object.fromEntries(series.contributors.map((c) => [c.memberId, c.shareAmount]))
  )
  const [intervalType, setIntervalType] = useState<RecurrenceInterval>(series.intervalType)
  const [intervalCount, setIntervalCount] = useState(String(series.intervalCount))
  const [anchorDate, setAnchorDate] = useState(series.anchorDate)
  const [endDate, setEndDate] = useState(series.endDate ?? '')
  const [error, setError] = useState<string | null>(null)

  function toggleContributor(memberId: string) {
    setContributorIds((ids) => (ids.includes(memberId) ? ids.filter((id) => id !== memberId) : [...ids, memberId]))
  }

  function selectType(next: TransactionType) {
    setType(next)
    const stillValid = categories.some((c) => c.id === categoryId && c.type === next)
    if (!stillValid) {
      setCategoryId(categories.find((c) => c.type === next)?.id ?? '')
    }
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
    if (endDate && endDate < anchorDate) {
      setError(t('recurring_series.end_date_before_start'))
      return
    }
    // No backlog check here, unlike the creation form: the server measures what a series
    // still owes from its materialization cursor, which this payload does not carry. Deriving
    // it from the anchor instead would refuse edits the server accepts — on an established
    // series the cursor has long moved past the anchor. Left to the server on purpose.
    setError(null)
    onSubmit({
      label: label.trim(), amount: numericAmount, type, categoryId, payerId: payerId || null,
      contributors: resolution.contributors,
      recurrence: { intervalType, intervalCount: Number(intervalCount), anchorDate, endDate: endDate || null },
    })
  }

  return (
    <Dialog open onClose={onCancel} title={t('recurring_series.edit_title')} maxWidth="max-w-lg">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <div className="flex gap-1 rounded-[11px] bg-bg-2 p-1">
          <button type="button" onClick={() => selectType('EXPENSE')} aria-pressed={type === 'EXPENSE'}
            className={`flex-1 rounded-[8px] px-3 py-2 text-sm font-semibold transition-colors ${type === 'EXPENSE' ? 'bg-bg-1 text-fg-0 shadow-sm' : 'text-fg-3'}`}>
            {t('type.EXPENSE')}
          </button>
          <button type="button" onClick={() => selectType('INCOME')} aria-pressed={type === 'INCOME'}
            className={`flex-1 rounded-[8px] px-3 py-2 text-sm font-semibold transition-colors ${type === 'INCOME' ? 'bg-bg-1 text-fg-0 shadow-sm' : 'text-fg-3'}`}>
            {t('type.INCOME')}
          </button>
        </div>

        <Input label={t('form.label_label')} value={label} onChange={(e) => setLabel(e.target.value)} />
        <Input label={t('form.amount_label')} type="number" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} />

        <div className="flex flex-col gap-1.5">
          <label htmlFor="series-category" className="text-[13px] font-semibold text-fg-1">{t('form.category_label')}</label>
          <select id="series-category" value={categoryId} onChange={(e) => setCategoryId(e.target.value)} className={SELECT_CLASSNAME}>
            {categories.filter((c) => c.type === type).map((c) => <option key={c.id} value={c.id}>{c.label}</option>)}
          </select>
        </div>

        {canPickContributors && (
          <ContributorsPicker
            members={members} payerId={payerId} onPayerChange={setPayerId}
            contributorIds={contributorIds} onToggleContributor={toggleContributor}
            customizeShares={customizeShares} onCustomizeSharesChange={setCustomizeShares}
            customShares={customShares}
            onCustomShareChange={(id, value) => setCustomShares((shares) => ({ ...shares, [id]: value }))}
          />
        )}

        <div className="flex flex-col gap-3 rounded-[10px] bg-bg-2 p-3">
          <div className="flex items-end gap-2">
            <Input label={t('form.recurrence_interval_count_label')} type="number" min={1}
              value={intervalCount} className="w-20"
              onChange={(e) => setIntervalCount(e.target.value)} />
            <select
              aria-label={t('form.recurrence_interval_type_label')}
              value={intervalType} onChange={(e) => setIntervalType(e.target.value as RecurrenceInterval)}
              className={SELECT_CLASSNAME}>
              <option value="DAILY">{t('form.interval.DAILY')}</option>
              <option value="WEEKLY">{t('form.interval.WEEKLY')}</option>
              <option value="MONTHLY">{t('form.interval.MONTHLY')}</option>
              <option value="YEARLY">{t('form.interval.YEARLY')}</option>
            </select>
          </div>
          <Input label={t('form.date_label')} type="date" value={anchorDate} onChange={(e) => setAnchorDate(e.target.value)} />
          <Input label={t('recurring_series.end_date_label')} type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
        </div>

        {(error ?? submitError) && <p className="text-sm font-medium text-status-red">{error ?? submitError}</p>}

        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" onClick={onCancel}>{t('form.cancel')}</Button>
          <Button type="submit" style={CTA_BUTTON_STYLE}>{t('form.save')}</Button>
        </div>
      </form>
    </Dialog>
  )
}
