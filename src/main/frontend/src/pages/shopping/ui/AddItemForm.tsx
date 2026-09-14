import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Plus } from 'lucide-react'
import { Input } from '@/shared/ui'
import { MEASUREMENT_UNITS, MEASUREMENT_UNIT_LABEL_KEY, type MeasurementUnit } from '@/shared/lib'
import type { ShoppingCategory } from '@/entities/shopping-list'

export interface NewItemInput {
  categoryId: string
  name: string
  quantity: number | null
  unit: MeasurementUnit | null
}

interface AddItemFormProps {
  categories: ShoppingCategory[]
  onAdd: (input: NewItemInput) => Promise<unknown>
  /** Reported upwards because the message belongs to the page's banner area, above the count. */
  onQuantityError: (invalid: boolean) => void
}

/**
 * The four fields of "add an item", and the drafts behind them.
 *
 * <p>They are here rather than on the page for a reason that was measured: held there, one keystroke
 * re-rendered the page, and the page renders every line of the list — **thirty line renders per
 * character** on a thirty-item list. Memoising the rows does not help, and the measurement is what
 * showed it: no row prop changes, the re-render comes from the drag-and-drop context the rows subscribe
 * to, and React.memo does not stop a context change. Not re-rendering the page at all is what works.
 */
export function AddItemForm({ categories, onAdd, onQuantityError }: AddItemFormProps) {
  const { t } = useTranslation('shopping')
  const { t: tCommon } = useTranslation('common')
  const [name, setName] = useState('')
  const [quantity, setQuantity] = useState('')
  const [unit, setUnit] = useState<MeasurementUnit | ''>('')
  const [categoryId, setCategoryId] = useState('')

  const effectiveCategoryId = categoryId || categories[0]?.id || ''

  function submit() {
    const trimmedName = name.trim()
    if (!trimmedName || !effectiveCategoryId) return
    const trimmedQuantity = quantity.trim()
    const parsed = trimmedQuantity === '' ? null : Number(trimmedQuantity)
    if (parsed != null && (!Number.isFinite(parsed) || parsed <= 0)) {
      onQuantityError(true)
      return
    }
    onQuantityError(false)
    void onAdd({ categoryId: effectiveCategoryId, name: trimmedName, quantity: parsed, unit: unit === '' ? null : unit })
      .then(() => {
        setName('')
        setQuantity('')
        setUnit('')
        setCategoryId(effectiveCategoryId)
      })
      .catch(() => {})
  }

  return (
    <div className="mb-6 flex flex-col gap-2 sm:flex-row sm:items-center">
      <Input
        label={t('add_item_placeholder')} srOnlyLabel
        value={name} onChange={(e) => setName(e.target.value)}
        onKeyDown={(e) => { if (e.key === 'Enter') submit() }}
        placeholder={t('add_item_placeholder')} aria-label={t('add_item_placeholder')} className="w-full sm:flex-1"
      />
      <div className="flex gap-2">
        <Input
          label={t('quantity_label')} srOnlyLabel type="number" min={0}
          value={quantity} onChange={(e) => setQuantity(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter') submit() }}
          placeholder={t('quantity_placeholder')} aria-label={t('quantity_label')} className="w-20"
        />
        <select value={unit} onChange={(e) => setUnit(e.target.value as MeasurementUnit | '')}
          aria-label={t('unit_label')} className="flex-1 rounded-[9px] border border-border bg-bg-1 px-2 py-2 text-xs sm:flex-none">
          <option value="">{t('unit_none')}</option>
          {MEASUREMENT_UNITS.map((u) => <option key={u} value={u}>{tCommon(MEASUREMENT_UNIT_LABEL_KEY[u])}</option>)}
        </select>
      </div>
      <select value={effectiveCategoryId} onChange={(e) => setCategoryId(e.target.value)}
        aria-label={t('category_label')} className="w-full rounded-[9px] border border-border bg-bg-1 px-2 py-2 text-xs sm:w-auto">
        {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
      </select>
      <button type="button" onClick={submit} aria-label={t('add_item')}
        className="flex w-full items-center justify-center gap-1.5 rounded-[10px] bg-accent px-3 py-2 text-xs font-semibold text-bg-0 sm:size-9 sm:w-9 sm:shrink-0 sm:p-0">
        <Plus className="size-4" /> <span className="sm:hidden">{t('add_item')}</span>
      </button>
    </div>
  )
}
