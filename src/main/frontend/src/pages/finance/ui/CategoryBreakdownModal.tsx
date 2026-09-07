import { useTranslation } from 'react-i18next'
import { Dialog } from '@/shared/ui'
import type { Category, CategoryAmount, TransactionType } from '@/entities/finance'
import { formatAmount } from '../lib/formatAmount'

interface CategoryBreakdownModalProps {
  type: TransactionType
  breakdown: CategoryAmount[]
  categoryById: Map<string, Category>
  onSelectCategory: (categoryId: string) => void
  onClose: () => void
}

/** The donut card's legend, without the donut — opened from a stat card so a category breakdown for one type is reachable without leaving the top of the page. */
export function CategoryBreakdownModal({ type, breakdown, categoryById, onSelectCategory, onClose }: CategoryBreakdownModalProps) {
  const { t } = useTranslation('finance')
  const filtered = breakdown.filter((row) => categoryById.get(row.categoryId)?.type === type)
  const total = filtered.reduce((sum, row) => sum + row.amount, 0)

  return (
    <Dialog open onClose={onClose} title={t(`breakdown.title_${type}`)} maxWidth="max-w-lg">
      <h3 className="mb-4 text-[19px] font-semibold text-fg-0">{t(`breakdown.title_${type}`)}</h3>

      {filtered.length === 0 ? (
        <p className="text-sm text-fg-3">{t(`breakdown.empty_${type}`)}</p>
      ) : (
        <ul className="space-y-2">
          {filtered.map((row) => {
            const category = categoryById.get(row.categoryId)
            const percent = total > 0 ? Math.round((row.amount / total) * 100) : 0
            return (
              <li key={row.categoryId}>
                <button type="button" onClick={() => onSelectCategory(row.categoryId)}
                  className="flex w-full items-center gap-2 rounded-lg p-1 text-left text-sm transition-colors hover:bg-bg-2">
                  <span className="size-2.5 shrink-0 rounded-[3px]" style={{ backgroundColor: category?.color }} />
                  <span className="flex-1 truncate text-fg-1">{category?.label ?? row.categoryId}</span>
                  <span className="text-fg-3">{percent}%</span>
                  <span className="w-20 text-right font-medium text-fg-0">{formatAmount(row.amount)}</span>
                </button>
              </li>
            )
          })}
        </ul>
      )}
    </Dialog>
  )
}
