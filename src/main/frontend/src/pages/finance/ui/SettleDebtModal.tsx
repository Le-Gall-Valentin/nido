import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Dialog, Button, Input, CTA_BUTTON_STYLE } from '@/shared/ui'
import { HandCoins } from 'lucide-react'

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
      <div className="mb-[15px] grid size-[46px] place-items-center rounded-[13px] bg-accent-dim text-accent">
        <HandCoins className="size-6" />
      </div>
      <h3 className="mb-2 text-[19px] font-semibold text-fg-0">{t('balances.settle_title')}</h3>
      <p className="mb-4 text-sm leading-relaxed text-fg-2">{t('balances.settle_message', { from: fromLabel, to: toLabel, amount })}</p>

      <Input label={t('balances.settle_date_label')} type="date" value={date} onChange={(e) => setDate(e.target.value)} />

      <div className="mt-5 flex justify-end gap-2">
        <Button type="button" onClick={onCancel} disabled={isPending}>{t('form.cancel')}</Button>
        <Button type="button" onClick={() => onConfirm(date)} isLoading={isPending} style={CTA_BUTTON_STYLE}>
          {t('balances.settle_confirm')}
        </Button>
      </div>
    </Dialog>
  )
}
