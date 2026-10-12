import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Dialog, Button, Input, CTA_BUTTON_STYLE } from '@/shared/ui'
import { parseAmount, todayIso } from '@/shared/lib'
import type { OpeningBalance } from '@/entities/finance'

interface OpeningBalanceModalProps {
  openingBalance: OpeningBalance | null
  /** The household's calendar: its today is the default date and the latest one allowed. */
  spaceTimezone?: string
  onSave: (amount: number, asOf: string) => void
  onClear: () => void
  onClose: () => void
  /** While a save or a removal is on its way: a second click would send it again. */
  isSaving?: boolean
  /** Set by the caller when the backend rejected the last save. */
  submitError?: string | null
}

export function OpeningBalanceModal({
  openingBalance, spaceTimezone, onSave, onClear, onClose, isSaving = false, submitError = null,
}: OpeningBalanceModalProps) {
  const { t } = useTranslation('finance')
  const today = todayIso(new Date(), spaceTimezone)
  const [asOf, setAsOf] = useState(openingBalance?.asOf ?? today)
  const [amount, setAmount] = useState(openingBalance ? String(openingBalance.amount) : '')
  const [error, setError] = useState<string | null>(null)

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    // A sign allowed: an overdraft is a balance too.
    const parsed = parseAmount(amount, { allowNegative: true })
    if ('problem' in parsed) {
      setError(t(`opening_balance.amount_${parsed.problem}`))
      return
    }
    if (!asOf) {
      setError(t('opening_balance.date_required'))
      return
    }
    if (asOf > today) {
      setError(t('opening_balance.date_in_future'))
      return
    }
    setError(null)
    onSave(parsed.value, asOf)
  }

  return (
    <Dialog open onClose={onClose} title={t('opening_balance.title')} maxWidth="max-w-md">
      {/* noValidate: the browser's own check of max would block the submission with a tooltip in its language, and
          the form's message would never show. max stays, to grey out the future in the picker. */}
      <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
        <h3 className="text-[19px] font-semibold text-fg-0">{t('opening_balance.title')}</h3>
        <p className="text-sm text-fg-2">{t('opening_balance.explanation')}</p>
        <Input label={t('opening_balance.date_label')} type="date" max={today} value={asOf} onChange={(e) => setAsOf(e.target.value)} />
        <Input label={t('opening_balance.amount_label')} type="number" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} />

        {(error ?? submitError) && <p className="text-sm font-medium text-status-red">{error ?? submitError}</p>}

        <div className="flex flex-wrap items-center justify-between gap-2 pt-2">
          {openingBalance ? (
            <button type="button" onClick={onClear} disabled={isSaving} className="text-sm font-semibold text-status-red disabled:opacity-50">
              {t('opening_balance.clear')}
            </button>
          ) : <span />}
          <div className="flex gap-2">
            <Button type="button" onClick={onClose}>{t('form.cancel')}</Button>
            <Button type="submit" isLoading={isSaving} style={CTA_BUTTON_STYLE}>{t('form.save')}</Button>
          </div>
        </div>
      </form>
    </Dialog>
  )
}
