import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Dialog } from '@/shared/ui'

interface SettleDebtModalProps {
  fromLabel: string
  toLabel: string
  amount: number
  onConfirm: (date: string) => void
  onCancel: () => void
  isPending: boolean
}

export function SettleDebtModal({ fromLabel, toLabel, amount, onConfirm, onCancel, isPending }: SettleDebtModalProps) {
  const { t } = useTranslation('finance')
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10))

  return (
    <Dialog open onClose={onCancel} title={t('balances.settle_title')}>
      <p className="text-sm">{t('balances.settle_message', { from: fromLabel, to: toLabel, amount })}</p>
      <div className="mt-3">
        <label htmlFor="settle-date" className="block text-sm font-medium">{t('balances.settle_date_label')}</label>
        <input id="settle-date" type="date" value={date} onChange={(e) => setDate(e.target.value)} className="mt-1 w-full rounded-md border px-3 py-2" />
      </div>
      <div className="mt-4 flex justify-end gap-2">
        <button type="button" onClick={onCancel} className="rounded-md px-4 py-2 text-sm">{t('form.cancel')}</button>
        <button type="button" onClick={() => onConfirm(date)} disabled={isPending} className="rounded-md bg-fg-1 px-4 py-2 text-sm text-bg-1 disabled:opacity-50">
          {t('balances.settle_confirm')}
        </button>
      </div>
    </Dialog>
  )
}
