import { useTranslation } from 'react-i18next'
import { Plus, Settings2 } from 'lucide-react'

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
      <div className="flex w-full flex-col gap-2 sm:w-auto sm:flex-row sm:flex-wrap sm:items-center">
        <input type="month" value={month} onChange={(e) => onMonthChange(e.target.value)}
          className="w-full rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3.5 py-2.5 text-sm text-fg-0 outline-none focus:border-accent sm:w-auto" />
        {canWrite && (
          <button type="button" onClick={onManageCategories}
            className="flex w-full items-center justify-center gap-1.5 rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3.5 py-2.5 text-sm font-semibold text-fg-2 hover:bg-bg-2 sm:w-auto">
            <Settings2 size={16} /> {t('categories.manage')}
          </button>
        )}
        {canWrite && (
          <button type="button" onClick={onNewTransaction}
            className="flex w-full items-center justify-center gap-1.5 rounded-[10px] bg-accent px-4 py-2.5 text-sm font-semibold text-white sm:w-auto">
            <Plus size={16} /> {t('new_transaction')}
          </button>
        )}
      </div>
    </div>
  )
}
