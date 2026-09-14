import { useTranslation } from 'react-i18next'
import { useSetBudget, useDeleteBudget, type BudgetLine, type Category } from '@/entities/finance'
import { BudgetManagerModal } from './BudgetManagerModal'

interface BudgetManagerPanelProps {
  spaceId: string
  categories: Category[]
  budgetLines: BudgetLine[]
  onClose: () => void
}

/** Setting and clearing the monthly ceilings — the two writes the budget screen needs, and nothing else. */
export function BudgetManagerPanel({ spaceId, categories, budgetLines, onClose }: BudgetManagerPanelProps) {
  const { t } = useTranslation('finance')
  const setBudget = useSetBudget(spaceId)
  const deleteBudget = useDeleteBudget(spaceId)

  return (
    <BudgetManagerModal
      categories={categories}
      budgetLines={budgetLines}
      onSave={(categoryId, monthlyLimit) => setBudget.mutate({ categoryId, monthlyLimit })}
      onDelete={(categoryId) => deleteBudget.mutate(categoryId)}
      onClose={onClose}
      submitError={setBudget.isError ? t('form.submit_error') : null}
    />
  )
}
