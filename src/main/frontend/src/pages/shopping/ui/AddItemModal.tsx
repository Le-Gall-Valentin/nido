import { useId, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Plus } from 'lucide-react'
import { Alert, Button, Dialog, Input } from '@/shared/ui'
import { MEASUREMENT_UNITS, MEASUREMENT_UNIT_LABEL_KEY, type MeasurementUnit } from '@/shared/lib'
import type { ShoppingCategory } from '@/entities/shopping-list'

export interface NewItemInput {
  categoryId: string
  name: string
  quantity: number | null
  unit: MeasurementUnit | null
}

interface AddItemModalProps {
  categories: ShoppingCategory[]
  onAdd: (input: NewItemInput) => Promise<unknown>
  onClose: () => void
}

const SELECT_CLASS = 'w-full rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3 py-[11px] text-[14.5px] text-fg-0 outline-none transition-colors hover:border-border-2 focus:border-accent'

/**
 * The four fields of "add an item", behind a button rather than pinned above the list.
 *
 * <p>The drafts stay inside this component, as they did in the barred-form version it replaces, and for
 * the same measured reason: held on the page, one keystroke re-rendered the page, and the page renders
 * every line of the list — **thirty line renders per character** on a thirty-item list. Memoising the
 * rows does not help: the re-render comes from the drag-and-drop context the rows subscribe to, and
 * {@code React.memo} does not stop a context change.
 *
 * <p>Being a modal strengthens that: the parent mounts it only while it is open, so the drafts reset on
 * close with no code, and neither the quantity error nor the failure message needs the page's banner
 * area — which the backdrop would cover anyway.
 */
export function AddItemModal({ categories, onAdd, onClose }: AddItemModalProps) {
  const { t } = useTranslation('shopping')
  const { t: tCommon } = useTranslation('common')
  const unitId = useId()
  const categoryId = useId()

  const [name, setName] = useState('')
  const [quantity, setQuantity] = useState('')
  const [unit, setUnit] = useState<MeasurementUnit | ''>('')
  const [category, setCategory] = useState('')
  const [quantityError, setQuantityError] = useState(false)
  const [failed, setFailed] = useState(false)
  const [pending, setPending] = useState(false)

  const effectiveCategoryId = category || categories[0]?.id || ''

  function submit() {
    const trimmedName = name.trim()
    if (!trimmedName || !effectiveCategoryId || pending) return
    const trimmedQuantity = quantity.trim()
    const parsed = trimmedQuantity === '' ? null : Number(trimmedQuantity)
    if (parsed != null && (!Number.isFinite(parsed) || parsed <= 0)) {
      setQuantityError(true)
      return
    }
    setQuantityError(false)
    setFailed(false)
    setPending(true)
    void onAdd({ categoryId: effectiveCategoryId, name: trimmedName, quantity: parsed, unit: unit === '' ? null : unit })
      .then(() => onClose())
      .catch(() => { setFailed(true); setPending(false) })
  }

  return (
    <Dialog open onClose={onClose} title={t('add_item_modal_title')}>
      <h3 className="mb-4 text-[19px] font-semibold text-fg-0">{t('add_item_modal_title')}</h3>

      {quantityError && <Alert variant="error" className="mb-3">{t('error.quantity_invalid')}</Alert>}
      {failed && <Alert variant="error" className="mb-3">{t('error.action_failed')}</Alert>}

      <div className="mb-5 flex flex-col gap-3">
        <Input
          label={t('add_item_name_label')} value={name} autoFocus
          onChange={(e) => setName(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter') submit() }}
          placeholder={t('add_item_placeholder')}
        />

        <div className="flex gap-3">
          <div className="w-24 shrink-0">
            <Input
              label={t('quantity_label')} type="number" min={0} value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter') submit() }}
              placeholder={t('quantity_placeholder')}
            />
          </div>
          <div className="flex flex-1 flex-col gap-1.5">
            <label htmlFor={unitId} className="text-[13px] font-semibold text-fg-1">{t('unit_label')}</label>
            <select id={unitId} value={unit} className={SELECT_CLASS}
              onChange={(e) => setUnit(e.target.value as MeasurementUnit | '')}>
              <option value="">{t('unit_none')}</option>
              {MEASUREMENT_UNITS.map((u) => <option key={u} value={u}>{tCommon(MEASUREMENT_UNIT_LABEL_KEY[u])}</option>)}
            </select>
          </div>
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor={categoryId} className="text-[13px] font-semibold text-fg-1">{t('category_label')}</label>
          <select id={categoryId} value={effectiveCategoryId} className={SELECT_CLASS}
            onChange={(e) => setCategory(e.target.value)}>
            {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </div>
      </div>

      <div className="flex justify-end gap-2">
        <Button type="button" onClick={onClose} disabled={pending}>{t('form_cancel')}</Button>
        <Button type="button" onClick={submit} isLoading={pending} className="border-transparent !text-bg-0" style={{ background: 'var(--color-accent)' }}>
          <Plus className="size-4" /> {t('add_item_confirm')}
        </Button>
      </div>
    </Dialog>
  )
}
