import { useTranslation } from 'react-i18next'
import { Plus, Settings2 } from 'lucide-react'
import { Button, PageActions } from '@/shared/ui'

interface FinanceHeaderProps {
  month: string
  onMonthChange: (month: string) => void
  canWrite: boolean
  onManageCategories: () => void
  onNewTransaction: () => void
}

export function FinanceHeader({ month, onMonthChange, canWrite, onManageCategories, onNewTransaction }: FinanceHeaderProps) {
  const { t } = useTranslation('finance')

  return (
    <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
      <h1 className="text-2xl font-bold text-fg-0">{t('title')}</h1>
      <PageActions>
        {/* As tall as the buttons beside it. */}
        <input type="month" value={month} onChange={(e) => onMonthChange(e.target.value)}
          className="rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3.5 py-[11px] text-sm text-fg-0 outline-none focus:border-accent" />
        {canWrite && (
          <Button type="button" icon={Settings2} onClick={onManageCategories}>{t('categories.manage')}</Button>
        )}
        {canWrite && (
          <Button type="button" variant="primary" icon={Plus} onClick={onNewTransaction}>{t('new_transaction')}</Button>
        )}
      </PageActions>
    </div>
  )
}
