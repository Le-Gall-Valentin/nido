import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { Dialog, Button, Alert, Input } from '@/shared/ui'
import { ROUTES } from '@/shared/config'
import {
  shoppingApi, ShoppingApiProvider, useShoppingCategories, useImportFromMenu, type IShoppingApi,
} from '@/entities/shopping-list'

/** A suggested ingredient line, already formatted by the caller — this feature has no notion of recipes or measurement units. */
export interface ExportableShoppingLine {
  name: string
  formattedQuantity: string
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
  const { data: categories } = useShoppingCategories(spaceId)
  const importFromMenu = useImportFromMenu(spaceId)

  const [checked, setChecked] = useState<boolean[]>(() => shoppingList.map(() => true))
  const [quantityDrafts, setQuantityDrafts] = useState<string[]>(() => shoppingList.map((line) => line.formattedQuantity))
  const [rowCategoryId, setRowCategoryId] = useState<string[]>(() => shoppingList.map(() => ''))
  const [bulkCategoryId, setBulkCategoryId] = useState('')
  const [error, setError] = useState(false)
  const [imported, setImported] = useState(false)

  useEffect(() => {
    if (categories && categories.length > 0 && rowCategoryId.every((id) => id === '')) {
      setBulkCategoryId(categories[0].id)
      setRowCategoryId(shoppingList.map(() => categories[0].id))
    }
  }, [categories, rowCategoryId, shoppingList])

  function applyBulkCategory(categoryId: string) {
    setBulkCategoryId(categoryId)
    setRowCategoryId(shoppingList.map(() => categoryId))
  }

  function setRowCategory(index: number, categoryId: string) {
    setRowCategoryId((rows) => rows.map((id, i) => (i === index ? categoryId : id)))
  }

  function handleConfirm() {
    const lines = shoppingList
      .map((line, i) => ({ line, i }))
      .filter(({ i }) => checked[i])
      .map(({ line, i }) => ({ name: line.name, quantityLabel: quantityDrafts[i], categoryId: rowCategoryId[i] }))
    setError(false)
    importFromMenu.mutateAsync(lines)
      .then(() => { setImported(true); onImported() })
      .catch(() => setError(true))
  }

  return (
    <Dialog open onClose={onClose} title={t('title')}>
      <p className="mb-4 text-xs text-fg-3">{t('subtitle')}</p>

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
          <div key={line.name} className="flex items-center gap-2 border-b border-border pb-2">
            <input type="checkbox" checked={checked[i]} aria-label={t('include_line', { name: line.name })}
              onChange={(e) => setChecked((c) => c.map((v, idx) => (idx === i ? e.target.checked : v)))} />
            <span className="flex-1 text-sm text-fg-1">{line.name}</span>
            <Input label={t('quantity_for', { name: line.name })} srOnlyLabel value={quantityDrafts[i]} className="w-28 text-xs"
              onChange={(e) => setQuantityDrafts((d) => d.map((v, idx) => (idx === i ? e.target.value : v)))} />
            <select value={rowCategoryId[i]} onChange={(e) => setRowCategory(i, e.target.value)}
              aria-label={t('category_for', { name: line.name })} className="rounded-md border border-border bg-bg-1 px-2 py-1 text-xs">
              {(categories ?? []).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
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
