import { useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Plus, Trash2, X, Check } from 'lucide-react'
import { Alert, Spinner, Input } from '@/shared/ui'
import { useMySpaces } from '@/features/space-switcher'
import { canWrite } from '@/entities/space'
import { shoppingApi } from '../api/shoppingApi'
import type { IShoppingApi } from '../model/IShoppingApi'
import { ShoppingApiProvider } from '../model/shoppingApiContext'
import { useShoppingCategories } from '../model/useShoppingCategories'
import { useShoppingItems } from '../model/useShoppingItems'
import { useCreateCategory, useRenameCategory, useDeleteCategory } from '../model/useCategoryMutations'
import { useAddItem, useUpdateItem, useToggleItemDone, useDeleteItem, useClearDoneItems, useClearAllItems } from '../model/useItemMutations'
import type { ShoppingItem } from '../model/types'

interface ShoppingListPageProps {
  api?: IShoppingApi
}

export function ShoppingListPage({ api = shoppingApi }: ShoppingListPageProps = {}) {
  return (
    <ShoppingApiProvider api={api}>
      <ShoppingListPageContent />
    </ShoppingApiProvider>
  )
}

function ShoppingListPageContent() {
  const { t } = useTranslation('shopping')
  const { spaceId = '' } = useParams<{ spaceId: string }>()
  const { data: categories, isPending, isError } = useShoppingCategories(spaceId)
  const { data: items } = useShoppingItems(spaceId)
  const { data: mySpaces } = useMySpaces()

  const createCategory = useCreateCategory(spaceId)
  const renameCategory = useRenameCategory(spaceId)
  const deleteCategory = useDeleteCategory(spaceId)
  const addItem = useAddItem(spaceId)
  const updateItem = useUpdateItem(spaceId)
  const toggleItemDone = useToggleItemDone(spaceId)
  const deleteItem = useDeleteItem(spaceId)
  const clearDoneItems = useClearDoneItems(spaceId)
  const clearAllItems = useClearAllItems(spaceId)

  const currentSpace = mySpaces?.find((s) => s.id === spaceId)
  const canWriteHere = currentSpace ? canWrite(currentSpace.myRole) : false

  const [actionError, setActionError] = useState(false)
  const [newItemName, setNewItemName] = useState('')
  const [newItemCategoryId, setNewItemCategoryId] = useState('')
  const [newCategoryName, setNewCategoryName] = useState('')
  const [renamingId, setRenamingId] = useState<string | null>(null)
  const [renameDraft, setRenameDraft] = useState('')

  const itemsByCategory = useMemo(() => {
    const map = new Map<string, ShoppingItem[]>()
    for (const item of items ?? []) {
      map.set(item.categoryId, [...(map.get(item.categoryId) ?? []), item])
    }
    return map
  }, [items])

  const effectiveCategoryId = newItemCategoryId || categories?.[0]?.id || ''

  function runMutation<T>(promise: Promise<T>) {
    setActionError(false)
    return promise.catch((error) => { setActionError(true); throw error })
  }

  function handleAddItem() {
    const name = newItemName.trim()
    if (!name || !effectiveCategoryId) return
    runMutation(addItem.mutateAsync({ categoryId: effectiveCategoryId, name }))
      .then(() => { setNewItemName(''); setNewItemCategoryId(effectiveCategoryId) })
      .catch(() => {})
  }

  function handleCreateCategory() {
    const name = newCategoryName.trim()
    if (!name) return
    runMutation(createCategory.mutateAsync(name)).then(() => setNewCategoryName('')).catch(() => {})
  }

  function confirmRename() {
    if (!renamingId) return
    const name = renameDraft.trim()
    if (!name) return
    runMutation(renameCategory.mutateAsync({ categoryId: renamingId, name })).then(() => setRenamingId(null)).catch(() => {})
  }

  if (isPending) return <Spinner label={t('loading')} fullscreen={false} />
  if (isError) return <Alert variant="error">{t('error.load_failed')}</Alert>

  const remaining = (items ?? []).filter((i) => !i.done).length

  return (
    <div className="mx-auto max-w-[720px] px-5 py-6 md:px-10 md:py-[34px]">
      <div className="mb-5 flex items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-fg-0">{t('title')}</h1>
        {(items ?? []).length > 0 && canWriteHere && (
          <div className="flex gap-2">
            <button type="button" onClick={() => runMutation(clearDoneItems.mutateAsync()).catch(() => {})}
              className="rounded-[9px] border border-border px-3 py-1.5 text-xs font-semibold text-fg-2">
              {t('clear_done')}
            </button>
            <button type="button" onClick={() => runMutation(clearAllItems.mutateAsync()).catch(() => {})}
              className="rounded-[9px] border border-status-red px-3 py-1.5 text-xs font-semibold text-status-red">
              {t('clear_all')}
            </button>
          </div>
        )}
      </div>

      {actionError && <Alert variant="error">{t('error.action_failed')}</Alert>}

      <p className="mb-4 text-xs text-fg-3">{t('remaining_count', { count: remaining })}</p>

      {canWriteHere && (
        <div className="mb-6 flex items-center gap-2">
          <Input
            label={t('add_item_placeholder')} srOnlyLabel
            value={newItemName} onChange={(e) => setNewItemName(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter') handleAddItem() }}
            placeholder={t('add_item_placeholder')} aria-label={t('add_item_placeholder')} className="flex-1"
          />
          <select value={effectiveCategoryId} onChange={(e) => setNewItemCategoryId(e.target.value)}
            aria-label={t('category_label')} className="rounded-[9px] border border-border bg-bg-1 px-2 py-2 text-xs">
            {(categories ?? []).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
          <button type="button" onClick={handleAddItem} aria-label={t('add_item')}
            className="grid size-9 place-items-center rounded-[10px] bg-accent text-bg-0">
            <Plus className="size-4" />
          </button>
        </div>
      )}

      {(items ?? []).length === 0 ? (
        <p className="text-sm text-fg-3">{t('empty')}</p>
      ) : (
        <div className="flex flex-col gap-5">
          {(categories ?? []).map((category) => {
            const categoryItems = itemsByCategory.get(category.id) ?? []
            if (categoryItems.length === 0) return null
            return (
              <div key={category.id}>
                <div className="mb-1.5 flex items-center gap-2">
                  {renamingId === category.id ? (
                    <>
                      <Input label={t('category_rename')} srOnlyLabel value={renameDraft} onChange={(e) => setRenameDraft(e.target.value)}
                        onKeyDown={(e) => { if (e.key === 'Enter') confirmRename() }} className="h-7 text-xs" />
                      <button type="button" onClick={confirmRename} aria-label={t('category_rename_confirm')}><Check className="size-3.5" /></button>
                      <button type="button" onClick={() => setRenamingId(null)} aria-label={t('form_cancel')}><X className="size-3.5" /></button>
                    </>
                  ) : (
                    <>
                      <span className="text-xs font-semibold uppercase tracking-wide text-fg-3">{category.name}</span>
                      <span className="text-xs text-fg-4">
                        {t('category_remaining', { count: categoryItems.filter((i) => !i.done).length })}
                      </span>
                      {canWriteHere && (
                        <div className="ml-auto flex gap-1">
                          <button type="button" onClick={() => { setRenamingId(category.id); setRenameDraft(category.name) }}
                            aria-label={t('category_rename')} className="text-fg-3">
                            {t('category_rename')}
                          </button>
                          {!category.fallback && (
                            <button type="button"
                              onClick={() => runMutation(deleteCategory.mutateAsync(category.id)).catch(() => {})}
                              aria-label={t('category_delete', { name: category.name })} className="text-status-red">
                              <Trash2 className="size-3.5" />
                            </button>
                          )}
                        </div>
                      )}
                    </>
                  )}
                </div>
                <div className="rounded-2xl border border-border bg-bg-1">
                  {categoryItems.map((item) => (
                    <div key={item.id} className="flex items-center gap-2 border-b border-border px-4 py-2.5 last:border-b-0">
                      <button type="button" onClick={() => runMutation(toggleItemDone.mutateAsync(item.id)).catch(() => {})}
                        aria-label={t('toggle_done', { name: item.name })} className="grid size-5 place-items-center rounded-md border border-border">
                        {item.done && <Check className="size-3.5 text-accent" />}
                      </button>
                      <span className={`flex-1 text-sm ${item.done ? 'text-fg-4 line-through' : 'text-fg-1'}`}>{item.name}</span>
                      {item.quantityLabel && <span className="text-xs text-fg-3">{item.quantityLabel}</span>}
                      {canWriteHere && (
                        <select value={item.categoryId}
                          onChange={(e) => runMutation(updateItem.mutateAsync({
                            itemId: item.id, categoryId: e.target.value, name: item.name, quantityLabel: item.quantityLabel,
                          })).catch(() => {})}
                          aria-label={t('recategorize', { name: item.name })} className="rounded-md border border-border bg-bg-1 px-1 py-1 text-xs">
                          {(categories ?? []).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                        </select>
                      )}
                      {canWriteHere && (
                        <button type="button" onClick={() => runMutation(deleteItem.mutateAsync(item.id)).catch(() => {})}
                          aria-label={t('delete_item', { name: item.name })} className="p-1 text-fg-3 hover:text-status-red">
                          <Trash2 className="size-3.5" />
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )
          })}
        </div>
      )}

      {canWriteHere && (
        <div className="mt-6 flex items-center gap-2">
          <Input label={t('new_category_placeholder')} srOnlyLabel value={newCategoryName} onChange={(e) => setNewCategoryName(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter') handleCreateCategory() }}
            placeholder={t('new_category_placeholder')} aria-label={t('new_category_placeholder')} className="max-w-[220px]" />
          <button type="button" onClick={handleCreateCategory}
            className="flex items-center gap-1 rounded-[9px] border border-dashed border-border-2 px-3 py-2 text-xs font-semibold text-fg-3">
            <Plus className="size-3.5" /> {t('new_category')}
          </button>
        </div>
      )}
    </div>
  )
}
