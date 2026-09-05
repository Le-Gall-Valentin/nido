import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Dialog } from '@/shared/ui'
import type { Category, BudgetLine } from '@/entities/finance'
import { CategoryIconBadge } from './CategoryIconBadge'
import { formatAmount } from '../lib/formatAmount'

interface BudgetManagerModalProps {
  categories: Category[]
  budgetLines: BudgetLine[]
  onSave: (categoryId: string, monthlyLimit: number) => void
  onClose: () => void
}

export function BudgetManagerModal({ categories, budgetLines, onSave, onClose }: BudgetManagerModalProps) {
  const { t } = useTranslation('finance')
  const [editingId, setEditingId] = useState<string | null>(null)
  const [budgetInput, setBudgetInput] = useState('')

  function startEdit(categoryId: string, currentLimit: number | null) {
    setEditingId(categoryId)
    setBudgetInput(currentLimit === null ? '' : String(currentLimit))
  }

  function handleSave(categoryId: string) {
    const value = Number(budgetInput)
    if (budgetInput.trim() !== '' && !Number.isNaN(value) && value >= 0) {
      onSave(categoryId, value)
    }
    setEditingId(null)
  }

  return (
    <Dialog open onClose={onClose} title={t('budget.manage_title')} maxWidth="max-w-lg">
      <h3 className="mb-4 text-[19px] font-semibold text-fg-0">{t('budget.manage_title')}</h3>

      <ul className="max-h-96 space-y-1 overflow-y-auto">
        {categories.map((category) => {
          const line = budgetLines.find((l) => l.categoryId === category.id)
          const currentLimit = line?.monthlyLimit ?? null

          if (editingId === category.id) {
            return (
              <li key={category.id} className="flex items-center gap-3 rounded-[10px] bg-bg-2 p-2">
                <CategoryIconBadge category={category} size={32} />
                <span className="flex-1 truncate text-sm font-medium text-fg-0">{category.label}</span>
                <input type="number" step="0.01" value={budgetInput} onChange={(e) => setBudgetInput(e.target.value)}
                  className="w-24 rounded-[8px] border-[1.5px] border-border bg-bg-1 px-2 py-1 text-sm text-fg-0 outline-none focus:border-accent" />
                <button type="button" onClick={() => handleSave(category.id)} className="text-sm font-semibold text-accent">{t('form.save')}</button>
              </li>
            )
          }
          return (
            <li key={category.id} className="flex items-center gap-3 rounded-[10px] p-2 hover:bg-bg-2">
              <CategoryIconBadge category={category} size={32} />
              <span className="flex-1 truncate text-sm font-medium text-fg-0">{category.label}</span>
              {currentLimit === null ? (
                <button type="button" onClick={() => startEdit(category.id, null)} className="text-xs font-semibold text-accent">
                  {t('budget.set')}
                </button>
              ) : (
                <span className="text-sm text-fg-2">
                  {formatAmount(currentLimit)}
                  <button type="button" onClick={() => startEdit(category.id, currentLimit)}
                    className="ml-2 text-xs font-semibold text-fg-3 hover:text-fg-1">{t('budget.edit')}</button>
                </span>
              )}
            </li>
          )
        })}
      </ul>
    </Dialog>
  )
}
