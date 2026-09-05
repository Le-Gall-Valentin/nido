import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Dialog } from '@/shared/ui'
import type { SpaceMember } from '@/entities/space'
import type { Category, ContributionInput, RecurrenceInput, Transaction, TransactionType } from '@/entities/finance'
import { resolveContributionShares } from '../lib/resolveContributionShares'

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
  onSubmit: (input: TransactionFormInput) => void
  onCancel: () => void
}

export function TransactionFormModal({ mode, transaction, categories, members, canPickContributors, onSubmit, onCancel }: TransactionFormModalProps) {
  const { t } = useTranslation('finance')
  const [label, setLabel] = useState(transaction?.label ?? '')
  const [amount, setAmount] = useState(transaction ? String(transaction.amount) : '')
  const [type, setType] = useState<TransactionType>(transaction?.type ?? 'EXPENSE')
  const [categoryId, setCategoryId] = useState(transaction?.categoryId ?? categories[0]?.id ?? '')
  const [date, setDate] = useState(transaction?.date ?? new Date().toISOString().slice(0, 10))
  const [payerId, setPayerId] = useState<string>(transaction?.payerId ?? '')
  const [contributorIds, setContributorIds] = useState<string[]>(transaction?.contributors.map((c) => c.memberId) ?? []);
  const [customizeShares, setCustomizeShares] = useState(false)
  const [customShares, setCustomShares] = useState<Record<string, number>>({})
  const [recurring, setRecurring] = useState(false)
  const [intervalType, setIntervalType] = useState<'DAILY' | 'WEEKLY' | 'MONTHLY'>('MONTHLY')
  const [intervalCount, setIntervalCount] = useState('1')
  const [anchorDate, setAnchorDate] = useState(new Date().toISOString().slice(0, 10))
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
    const resolved = resolveContributionShares(numericAmount, contributorIds, customizeShares ? customShares : null)
    if (resolved === null) {
      setError(t('form.shares_invalid'))
      return
    }
    setError(null)
    onSubmit({
      label: label.trim(), amount: numericAmount, type, categoryId, date, payerId: payerId || null,
      contributors: resolved,
      recurrence: recurring ? { intervalType, intervalCount: Number(intervalCount), anchorDate, endDate: null } : null,
    })
  }

  return (
    <Dialog open onClose={onCancel} title={mode === 'create' ? t('form.create_title') : t('form.edit_title')}>
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && <p className="text-sm text-red-600">{error}</p>}
        <div className="flex gap-2">
          <button type="button" onClick={() => setType('EXPENSE')} aria-pressed={type === 'EXPENSE'}
            className={`flex-1 rounded-md px-3 py-2 text-sm ${type === 'EXPENSE' ? 'bg-red-100 text-red-700' : 'bg-bg-2'}`}>
            {t('type.EXPENSE')}
          </button>
          <button type="button" onClick={() => setType('INCOME')} aria-pressed={type === 'INCOME'}
            className={`flex-1 rounded-md px-3 py-2 text-sm ${type === 'INCOME' ? 'bg-green-100 text-green-700' : 'bg-bg-2'}`}>
            {t('type.INCOME')}
          </button>
        </div>
        <div>
          <label htmlFor="finance-label" className="block text-sm font-medium">{t('form.label_label')}</label>
          <input id="finance-label" value={label} onChange={(e) => setLabel(e.target.value)} className="mt-1 w-full rounded-md border px-3 py-2" />
        </div>
        <div>
          <label htmlFor="finance-amount" className="block text-sm font-medium">{t('form.amount_label')}</label>
          <input id="finance-amount" type="number" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} className="mt-1 w-full rounded-md border px-3 py-2" />
        </div>
        <div>
          <label htmlFor="finance-category" className="block text-sm font-medium">{t('form.category_label')}</label>
          <select id="finance-category" value={categoryId} onChange={(e) => setCategoryId(e.target.value)} className="mt-1 w-full rounded-md border px-3 py-2">
            {categories.map((c) => <option key={c.id} value={c.id}>{c.label}</option>)}
          </select>
        </div>
        <div>
          <label htmlFor="finance-date" className="block text-sm font-medium">{t('form.date_label')}</label>
          <input id="finance-date" type="date" value={date} onChange={(e) => setDate(e.target.value)} className="mt-1 w-full rounded-md border px-3 py-2" />
        </div>
        {canPickContributors && (
          <>
            <div>
              <label htmlFor="finance-payer" className="block text-sm font-medium">{t('form.payer_label')}</label>
              <select id="finance-payer" value={payerId} onChange={(e) => setPayerId(e.target.value)} className="mt-1 w-full rounded-md border px-3 py-2">
                <option value="">{t('form.payer_none')}</option>
                {members.map((m) => <option key={m.userId} value={m.userId}>{m.username ?? m.email}</option>)}
              </select>
            </div>
            <fieldset>
              <legend className="text-sm font-medium">{t('form.contributors_label')}</legend>
              {members.map((m) => (
                <label key={m.userId} className="flex items-center gap-2 text-sm">
                  <input type="checkbox" checked={contributorIds.includes(m.userId)} onChange={() => toggleContributor(m.userId)} />
                  {m.username ?? m.email}
                </label>
              ))}
            </fieldset>
            {contributorIds.length > 0 && (
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={customizeShares} onChange={(e) => setCustomizeShares(e.target.checked)} />
                {t('form.customize_shares_label')}
              </label>
            )}
            {customizeShares && contributorIds.map((id) => {
              const member = members.find((m) => m.userId === id)
              return (
                <div key={id} className="flex items-center gap-2">
                  <span className="w-32 text-sm">{member?.username ?? member?.email}</span>
                  <input type="number" step="0.01" value={customShares[id] ?? ''}
                    onChange={(e) => setCustomShares((shares) => ({ ...shares, [id]: Number(e.target.value) }))}
                    className="w-24 rounded-md border px-2 py-1" />
                </div>
              )
            })}
          </>
        )}
        {mode === 'create' && (
          <>
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" checked={recurring} onChange={(e) => setRecurring(e.target.checked)} />
              {t('form.recurring_label')}
            </label>
            {recurring && (
              <div className="space-y-2 rounded-md border p-3">
                <div>
                  <label htmlFor="finance-interval-type" className="block text-sm font-medium">{t('form.recurrence_interval_type_label')}</label>
                  <select id="finance-interval-type" value={intervalType} onChange={(e) => setIntervalType(e.target.value as 'DAILY' | 'WEEKLY' | 'MONTHLY')} className="mt-1 w-full rounded-md border px-3 py-2">
                    <option value="DAILY">{t('interval.DAILY')}</option>
                    <option value="WEEKLY">{t('interval.WEEKLY')}</option>
                    <option value="MONTHLY">{t('interval.MONTHLY')}</option>
                  </select>
                </div>
                <div>
                  <label htmlFor="finance-interval-count" className="block text-sm font-medium">{t('form.recurrence_interval_count_label')}</label>
                  <input id="finance-interval-count" type="number" min="1" value={intervalCount} onChange={(e) => setIntervalCount(e.target.value)} className="mt-1 w-full rounded-md border px-3 py-2" />
                </div>
                <div>
                  <label htmlFor="finance-anchor-date" className="block text-sm font-medium">{t('form.recurrence_anchor_date_label')}</label>
                  <input id="finance-anchor-date" type="date" value={anchorDate} onChange={(e) => setAnchorDate(e.target.value)} className="mt-1 w-full rounded-md border px-3 py-2" />
                </div>
              </div>
            )}
          </>
        )}
        <div className="flex justify-end gap-2 pt-2">
          <button type="button" onClick={onCancel} className="rounded-md px-4 py-2 text-sm">{t('form.cancel')}</button>
          <button type="submit" className="rounded-md bg-fg-1 px-4 py-2 text-sm text-bg-1">{t('form.save')}</button>
        </div>
      </form>
    </Dialog>
  )
}
