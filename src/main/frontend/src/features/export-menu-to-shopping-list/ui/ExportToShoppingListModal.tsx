import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { Dialog, Button, Alert, Input } from '@/shared/ui'
import { ROUTES } from '@/shared/config'
import { MEASUREMENT_UNITS, MEASUREMENT_UNIT_LABEL_KEY, type MeasurementUnit } from '@/shared/lib'
import {
  shoppingApi, ShoppingApiProvider, useShoppingCategories, useImportFromMenu, type IShoppingApi,
} from '@/entities/shopping-list'

/** A suggested ingredient line — this feature has no notion of recipes, only a name and an optional structured quantity. */
export interface ExportableShoppingLine {
  name: string
  quantity?: number | null
  unit?: MeasurementUnit | null
}

interface ExportToShoppingListModalProps {
  open: boolean
  onClose: () => void
  spaceId: string
  shoppingList: ExportableShoppingLine[]
  onImported: () => void
  api?: IShoppingApi
}

export function ExportToShoppingListModal({ open, onClose, spaceId, shoppingList, onImported, api = shoppingApi }: ExportToShoppingListModalProps) {
  if (!open) return null
  return (
    <ShoppingApiProvider api={api}>
      <ExportToShoppingListModalContent spaceId={spaceId} shoppingList={shoppingList} onClose={onClose} onImported={onImported} />
    </ShoppingApiProvider>
  )
}

function ExportToShoppingListModalContent({
  spaceId, shoppingList, onClose, onImported,
}: Omit<ExportToShoppingListModalProps, 'open' | 'api'>) {
  const { t } = useTranslation('exportMenuToShoppingList')
  const { t: tCommon } = useTranslation('common')
  const { data: categories } = useShoppingCategories(spaceId)
  const importFromMenu = useImportFromMenu(spaceId)

  const [checked, setChecked] = useState<boolean[]>(() => shoppingList.map(() => true))
  const [quantityDrafts, setQuantityDrafts] = useState<string[]>(() => shoppingList.map((line) => (line.quantity != null ? String(line.quantity) : '')))
  const [unitDrafts, setUnitDrafts] = useState<(MeasurementUnit | '')[]>(() => shoppingList.map((line) => line.unit ?? ''))
  const [rowCategoryId, setRowCategoryId] = useState<string[]>(() => shoppingList.map(() => ''))
  const [bulkCategoryId, setBulkCategoryId] = useState('')
  const [error, setError] = useState(false)
  const [quantityError, setQuantityError] = useState(false)
  const [imported, setImported] = useState(false)
  const defaultCategoryApplied = useRef(false)

  useEffect(() => {
    if (!defaultCategoryApplied.current && categories && categories.length > 0) {
      defaultCategoryApplied.current = true
      setBulkCategoryId(categories[0].id)
      setRowCategoryId(shoppingList.map(() => categories[0].id))
    }
  }, [categories, shoppingList])

  function applyBulkCategory(categoryId: string) {
    setBulkCategoryId(categoryId)
    setRowCategoryId(shoppingList.map(() => categoryId))
  }

  function setRowCategory(index: number, categoryId: string) {
    setRowCategoryId((rows) => rows.map((id, i) => (i === index ? categoryId : id)))
  }

  function setRowUnit(index: number, unit: MeasurementUnit | '') {
    setUnitDrafts((units) => units.map((u, i) => (i === index ? unit : u)))
  }

  function handleConfirm() {
    const included = shoppingList
      .map((line, i) => ({ line, i }))
      .filter(({ i }) => checked[i])

    const quantities = included.map(({ i }) => (quantityDrafts[i].trim() === '' ? null : Number(quantityDrafts[i])))
    if (quantities.some((quantity) => quantity != null && (!Number.isFinite(quantity) || quantity <= 0))) {
      setQuantityError(true)
      return
    }
    setQuantityError(false)

    const lines = included.map(({ line, i }, index) => ({
      name: line.name,
      quantity: quantities[index],
      unit: unitDrafts[i] === '' ? null : unitDrafts[i],
      categoryId: rowCategoryId[i],
    }))
    setError(false)
    importFromMenu.mutateAsync(lines)
      .then(() => { setImported(true); onImported() })
      .catch(() => setError(true))
  }

  return (
    <Dialog open onClose={onClose} title={t('title')}>
      <p className="mb-4 text-xs text-fg-3">{t('subtitle')}</p>

      {quantityError && <Alert variant="error" className="mb-3">{t('quantity_invalid')}</Alert>}
      {error && <Alert variant="error" className="mb-3">{t('error')}</Alert>}

      {(categories ?? []).length > 0 && (
        <div className="mb-3 flex items-center gap-2">
          <label className="text-xs font-semibold text-fg-3">{t('bulk_category_label')}</label>
          <select value={bulkCategoryId} onChange={(e) => applyBulkCategory(e.target.value)}
            aria-label={t('bulk_category_label')} className="rounded-md border border-border bg-bg-1 px-2 py-1 text-xs">
            {(categories ?? []).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </div>
      )}

      <div className="mb-4 flex max-h-[320px] flex-col gap-2 overflow-y-auto">
        {shoppingList.map((line, i) => (
          <div key={line.name} className="flex flex-col gap-2 border-b border-border pb-2 sm:flex-row sm:items-center">
            <div className="flex items-center gap-2 sm:flex-1">
              <input type="checkbox" checked={checked[i]} aria-label={t('include_line', { name: line.name })}
                onChange={(e) => setChecked((c) => c.map((v, idx) => (idx === i ? e.target.checked : v)))} />
              <span className="flex-1 text-sm text-fg-1">{line.name}</span>
            </div>
            <div className="flex flex-col gap-2 pl-6 sm:flex-row sm:items-center sm:pl-0">
              <div className="flex gap-2">
                <Input label={t('quantity_for', { name: line.name })} srOnlyLabel type="number" min={0} value={quantityDrafts[i]} className="w-20 shrink-0 text-xs"
                  onChange={(e) => setQuantityDrafts((d) => d.map((v, idx) => (idx === i ? e.target.value : v)))} />
                <select value={unitDrafts[i]} onChange={(e) => setRowUnit(i, e.target.value as MeasurementUnit | '')}
                  aria-label={t('unit_for', { name: line.name })} className="flex-1 rounded-md border border-border bg-bg-1 px-2 py-1 text-xs sm:flex-none">
                  <option value="">{t('unit_none')}</option>
                  {MEASUREMENT_UNITS.map((u) => <option key={u} value={u}>{tCommon(MEASUREMENT_UNIT_LABEL_KEY[u])}</option>)}
                </select>
              </div>
              <select value={rowCategoryId[i]} onChange={(e) => setRowCategory(i, e.target.value)}
                aria-label={t('category_for', { name: line.name })} className="w-full rounded-md border border-border bg-bg-1 px-2 py-1 text-xs sm:w-auto sm:flex-1">
                {(categories ?? []).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </div>
          </div>
        ))}
      </div>

      {imported ? (
        <p className="text-sm font-semibold text-status-green">
          {t('success')} · <Link to={ROUTES.spaceOrganisationCourses(spaceId)} className="underline">{t('view_shopping_list')}</Link>
        </p>
      ) : (
        <div className="flex justify-end gap-2">
          <Button type="button" onClick={onClose}>{t('cancel')}</Button>
          <Button type="button" onClick={handleConfirm} isLoading={importFromMenu.isPending}>{t('confirm')}</Button>
        </div>
      )}
    </Dialog>
  )
}
