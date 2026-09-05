import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Dialog, Button, Input, CTA_BUTTON_STYLE } from '@/shared/ui'
import { HandCoins } from 'lucide-react'

interface SettleDebtModalProps {
  fromLabel: string
  toLabel: string
  amount: number
  onConfirm: (amount: number, date: string) => void
  onCancel: () => void
  isPending: boolean
}

export function SettleDebtModal({ fromLabel, toLabel, amount, onConfirm, onCancel, isPending }: SettleDebtModalProps) {
  const { t } = useTranslation('finance')
  const [amountInput, setAmountInput] = useState(String(amount))
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10))
  const [error, setError] = useState<string | null>(null)

  function handleConfirm() {
    const numericAmount = Number(amountInput)
    if (!amountInput || Number.isNaN(numericAmount) || numericAmount <= 0) {
      setError(t('balances.settle_amount_required'))
      return
    }
    if (numericAmount > amount) {
      setError(t('balances.settle_amount_too_high'))
      return
    }
    setError(null)
    onConfirm(numericAmount, date)
  }

  return (
    <Dialog open onClose={onCancel} title={t('balances.settle_title')}>
      <div className="mb-[15px] grid size-[46px] place-items-center rounded-[13px] bg-accent-dim text-accent">
        <HandCoins className="size-6" />
      </div>
      <h3 className="mb-2 text-[19px] font-semibold text-fg-0">{t('balances.settle_title')}</h3>
      <p className="mb-4 text-sm leading-relaxed text-fg-2">{t('balances.settle_message', { from: fromLabel, to: toLabel, amount })}</p>

      <div className="flex flex-col gap-4">
        <Input label={t('balances.settle_amount_label')} type="number" step="0.01" min={0} max={amount}
          value={amountInput} onChange={(e) => setAmountInput(e.target.value)} />
        <Input label={t('balances.settle_date_label')} type="date" value={date} onChange={(e) => setDate(e.target.value)} />
      </div>

      {error && <p className="mt-2 text-sm font-medium text-status-red">{error}</p>}

      <div className="mt-5 flex justify-end gap-2">
        <Button type="button" onClick={onCancel} disabled={isPending}>{t('form.cancel')}</Button>
        <Button type="button" onClick={handleConfirm} isLoading={isPending} style={CTA_BUTTON_STYLE}>
          {t('balances.settle_confirm')}
        </Button>
      </div>
    </Dialog>
  )
}
