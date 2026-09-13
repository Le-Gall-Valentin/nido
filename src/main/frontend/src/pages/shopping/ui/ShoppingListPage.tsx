import { useCallback, useMemo, useState, type ReactNode } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Plus, Trash2, X, Check } from 'lucide-react'
import {
  DndContext, DragOverlay, useDroppable, PointerSensor, useSensor, useSensors,
  type DragEndEvent, type DragStartEvent,
} from '@dnd-kit/core'
import { Alert, Spinner, Input, Dialog } from '@/shared/ui'
import { MEASUREMENT_UNIT_LABEL_KEY } from '@/shared/lib'
import { useMySpaces } from '@/features/space-switcher'
import { canWrite } from '@/entities/space'
import {
  shoppingApi, ShoppingApiProvider, useShoppingCategories, useShoppingItems,
  useCreateCategory, useRenameCategory, useDeleteCategory,
  useAddItem, useUpdateItem, useToggleItemDone, useDeleteItem, useClearDoneItems, useClearAllItems,
  type IShoppingApi, type ShoppingItem,
} from '@/entities/shopping-list'
import { ShoppingItemRow } from './ShoppingItemRow'
import { AddItemForm, type NewItemInput } from './AddItemForm'
import { resolveItemMove } from './resolveItemMove'

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
  const { t: tCommon } = useTranslation('common')
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
  const [quantityError, setQuantityError] = useState(false)
  const [newCategoryName, setNewCategoryName] = useState('')
  const [renamingId, setRenamingId] = useState<string | null>(null)
  const [renameDraft, setRenameDraft] = useState('')
  const [movingItem, setMovingItem] = useState<ShoppingItem | null>(null)
  const [activeDragItem, setActiveDragItem] = useState<ShoppingItem | null>(null)

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 8 } }))

  // Stable on purpose: a memoised row compares its props, and a fresh arrow per render would make
  // that comparison fail every time. See ShoppingItemRow for the measurement.
  function formatQuantity(item: ShoppingItem): string | null {
    const parts = [
      item.quantity != null ? String(item.quantity) : null,
      item.unit ? tCommon(MEASUREMENT_UNIT_LABEL_KEY[item.unit]) : null,
    ].filter((p): p is string => p != null)
    return parts.length > 0 ? parts.join(' ') : null
  }

  const itemsByCategory = useMemo(() => {
    const map = new Map<string, ShoppingItem[]>()
    for (const item of items ?? []) {
      map.set(item.categoryId, [...(map.get(item.categoryId) ?? []), item])
    }
    return map
  }, [items])

  // Stable: the row handlers below are memoised on it, and a fresh function here would undo that.
  // setActionError is a state setter, so there is nothing else to depend on.
  const runMutation = useCallback(<T,>(promise: Promise<T>) => {
    setActionError(false)
    return promise.catch((error: unknown) => { setActionError(true); throw error })
  }, [])

  // mutateAsync keeps its identity across renders (the mutation object it hangs off does not), so
  // depending on the function rather than the object is what keeps these two stable.
  const { mutateAsync: toggleItemDoneAsync } = toggleItemDone
  const { mutateAsync: deleteItemAsync } = deleteItem

  const handleToggleItem = useCallback((itemId: string) => {
    runMutation(toggleItemDoneAsync(itemId)).catch(() => {})
  }, [runMutation, toggleItemDoneAsync])

  const handleDeleteItem = useCallback((itemId: string) => {
    runMutation(deleteItemAsync(itemId)).catch(() => {})
  }, [runMutation, deleteItemAsync])

  const { mutateAsync: addItemAsync } = addItem
  const handleAdd = useCallback((input: NewItemInput) => runMutation(addItemAsync(input)),
    [runMutation, addItemAsync])

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

  function moveItemToCategory(item: ShoppingItem, categoryId: string) {
    setMovingItem(null)
    runMutation(updateItem.mutateAsync({
      itemId: item.id, categoryId, name: item.name, quantity: item.quantity, unit: item.unit,
    })).catch(() => {})
  }

  function handleDragStart(event: DragStartEvent) {
    setActiveDragItem((items ?? []).find((i) => i.id === event.active.id) ?? null)
  }

  function handleDragEnd(event: DragEndEvent) {
    setActiveDragItem(null)
    const { active, over } = event
    if (!over) return
    const item = resolveItemMove(items ?? [], String(active.id), String(over.id))
    if (!item) return
    moveItemToCategory(item, String(over.id))
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
            <button type="button" onClick={() => { runMutation(clearDoneItems.mutateAsync()).catch(() => {}) }}
              className="rounded-[9px] border border-border px-3 py-1.5 text-xs font-semibold text-fg-2">
              {t('clear_done')}
            </button>
            <button type="button" onClick={() => { runMutation(clearAllItems.mutateAsync()).catch(() => {}) }}
              className="rounded-[9px] border border-status-red px-3 py-1.5 text-xs font-semibold text-status-red">
              {t('clear_all')}
            </button>
          </div>
        )}
      </div>

      {quantityError && <Alert variant="error">{t('error.quantity_invalid')}</Alert>}
      {actionError && <Alert variant="error">{t('error.action_failed')}</Alert>}

      <p className="mb-4 text-xs text-fg-3">{t('remaining_count', { count: remaining })}</p>

      {canWriteHere && (
        <AddItemForm
          categories={categories ?? []}
          onAdd={handleAdd}
          onQuantityError={setQuantityError}
        />
      )}

      {(items ?? []).length === 0 ? (
        <p className="text-sm text-fg-3">{t('empty')}</p>
      ) : (
        <DndContext sensors={sensors} onDragStart={handleDragStart} onDragEnd={handleDragEnd} onDragCancel={() => setActiveDragItem(null)}>
          <div className="flex flex-col gap-5">
            {(categories ?? []).map((category) => {
              const categoryItems = itemsByCategory.get(category.id) ?? []
              return (
                <CategoryDropZone key={category.id} categoryId={category.id}>
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
                                onClick={() => { runMutation(deleteCategory.mutateAsync(category.id)).catch(() => {}) }}
                                aria-label={t('category_delete', { name: category.name })} className="text-status-red">
                                <Trash2 className="size-3.5" />
                              </button>
                            )}
                          </div>
                        )}
                      </>
                    )}
                  </div>
                  {categoryItems.length > 0 && (
                    <div className="rounded-2xl border border-border bg-bg-1">
                      {categoryItems.map((item) => (
                        <ShoppingItemRow
                          key={item.id} item={item} canWrite={canWriteHere} quantityLabel={formatQuantity(item)}
                          onToggleDone={handleToggleItem}
                          onDelete={handleDeleteItem}
                          onRequestMove={setMovingItem}
                        />
                      ))}
                    </div>
                  )}
                </CategoryDropZone>
              )
            })}
          </div>
          <DragOverlay>
            {activeDragItem && (
              <div className="rounded-md border border-accent bg-bg-1 px-3 py-1.5 text-sm text-fg-1 shadow-lg">
                {activeDragItem.name}
              </div>
            )}
          </DragOverlay>
        </DndContext>
      )}

      {movingItem && (
        <Dialog open onClose={() => setMovingItem(null)} title={t('move_item_dialog_title', { name: movingItem.name })} showCloseButton>
          <p className="mb-3 pr-8 text-sm font-semibold text-fg-1">{t('move_item_dialog_title', { name: movingItem.name })}</p>
          <div className="flex flex-col gap-1">
            {(categories ?? []).map((c) => {
              const isCurrent = c.id === movingItem.categoryId
              return (
                <button key={c.id} type="button" disabled={isCurrent} onClick={() => moveItemToCategory(movingItem, c.id)}
                  className="rounded-md px-3 py-2 text-left text-sm hover:bg-bg-2 disabled:text-fg-4 disabled:hover:bg-transparent">
                  {isCurrent ? t('move_item_target_current', { category: c.name }) : c.name}
                </button>
              )
            })}
          </div>
        </Dialog>
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

interface CategoryDropZoneProps {
  categoryId: string
  children: ReactNode
}

// The whole category block (header + items, if any) is the drop target — this way an
// empty category never needs a placeholder box of its own: its header is always visible
// and is itself big enough to drop on.
function CategoryDropZone({ categoryId, children }: CategoryDropZoneProps) {
  const { setNodeRef, isOver } = useDroppable({ id: categoryId })
  return (
    <div ref={setNodeRef} className={`transition-colors ${isOver ? 'bg-accent/10' : ''}`}>
      {children}
    </div>
  )
}
