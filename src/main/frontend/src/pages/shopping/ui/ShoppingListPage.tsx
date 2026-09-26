import { useCallback, useMemo, useState, type ReactNode } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Plus, SlidersHorizontal } from 'lucide-react'
import { DndContext, DragOverlay, useDroppable, type DragEndEvent, type DragStartEvent } from '@dnd-kit/core'
import { Alert, Spinner, Dialog } from '@/shared/ui'
import { MEASUREMENT_UNIT_LABEL_KEY, underThePointerFirst, useDragSensors } from '@/shared/lib'
import { useMySpaces } from '@/features/space-switcher'
import { canWrite } from '@/entities/space'
import {
  shoppingApi, ShoppingApiProvider, useShoppingCategories, useShoppingItems,
  useCreateCategory, useRenameCategory, useDeleteCategory,
  useAddItem, useUpdateItem, useToggleItemDone, useDeleteItem, useClearDoneItems, useClearAllItems,
  type IShoppingApi, type ShoppingItem,
} from '@/entities/shopping-list'
import { ShoppingItemPreview, ShoppingItemRow } from './ShoppingItemRow'
import { AddItemModal } from './AddItemModal'
import { ManageCategoriesModal } from './ManageCategoriesModal'
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
  const [addingItem, setAddingItem] = useState(false)
  const [managingCategories, setManagingCategories] = useState(false)
  const [movingItem, setMovingItem] = useState<ShoppingItem | null>(null)
  const [activeDragItem, setActiveDragItem] = useState<ShoppingItem | null>(null)

  const sensors = useDragSensors()

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

  // Only the categories actually holding something. An empty category is a rangement decision, not a
  // line of the list — it belongs in the manage-categories modal, which is where it can be renamed or
  // removed. The trade-off: an empty category is no longer a drop target, so the move dialog (which
  // still lists every category) is the way into one.
  const filledCategories = useMemo(
    () => (categories ?? []).filter((c) => (itemsByCategory.get(c.id) ?? []).length > 0),
    [categories, itemsByCategory]
  )

  const itemCountByCategory = useMemo(() => {
    const map = new Map<string, number>()
    for (const [categoryId, categoryItems] of itemsByCategory) map.set(categoryId, categoryItems.length)
    return map
  }, [itemsByCategory])

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

      {actionError && <Alert variant="error">{t('error.action_failed')}</Alert>}

      <p className="mb-4 text-xs text-fg-3">{t('remaining_count', { count: remaining })}</p>

      {/* Two equal columns from sm up, one stacked column below: a grid rather than a flex row, so
          the pair keeps a single width whatever the two labels translate to. */}
      {canWriteHere && (
        <div className="mb-6 grid grid-cols-1 gap-2 sm:grid-cols-2">
          <button type="button" onClick={() => setAddingItem(true)}
            className="flex items-center justify-center gap-1.5 rounded-[10px] px-4 py-2.5 text-sm font-semibold text-bg-0"
            style={{ background: 'var(--color-accent)' }}>
            <Plus className="size-4" /> {t('add_item')}
          </button>
          <button type="button" onClick={() => setManagingCategories(true)}
            className="flex items-center justify-center gap-1.5 rounded-[10px] border-[1.5px] border-border px-4 py-2.5 text-sm font-semibold text-fg-2 transition-colors hover:bg-bg-2 hover:text-fg-0">
            <SlidersHorizontal className="size-4" /> {t('manage_categories')}
          </button>
        </div>
      )}

      {(items ?? []).length === 0 ? (
        <p className="text-sm text-fg-3">{t('empty')}</p>
      ) : (
        <DndContext sensors={sensors} collisionDetection={underThePointerFirst}
          onDragStart={handleDragStart} onDragEnd={handleDragEnd} onDragCancel={() => setActiveDragItem(null)}>
          <div className="flex flex-col gap-5">
            {filledCategories.map((category) => {
              const categoryItems = itemsByCategory.get(category.id) ?? []
              return (
                <CategoryDropZone key={category.id} categoryId={category.id}>
                  <div className="mb-1.5 flex items-center gap-2">
                    <span className="text-xs font-semibold uppercase tracking-wide text-fg-3">{category.name}</span>
                    <span className="text-xs text-fg-4">
                      {t('category_remaining', { count: categoryItems.filter((i) => !i.done).length })}
                    </span>
                  </div>
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
                </CategoryDropZone>
              )
            })}
          </div>
          {/* No drop animation: it would fly the copy back to the old category while the move is
              still on its way to the server. */}
          <DragOverlay dropAnimation={null}>
            {activeDragItem && <ShoppingItemPreview item={activeDragItem} quantityLabel={formatQuantity(activeDragItem)} />}
          </DragOverlay>
        </DndContext>
      )}

      {addingItem && (
        <AddItemModal
          categories={categories ?? []}
          onAdd={addItem.mutateAsync}
          onClose={() => setAddingItem(false)}
        />
      )}

      {managingCategories && (
        <ManageCategoriesModal
          categories={categories ?? []}
          itemCountByCategory={itemCountByCategory}
          onCreate={(name) => createCategory.mutateAsync(name)}
          onRename={(categoryId, name) => renameCategory.mutateAsync({ categoryId, name })}
          onDelete={(categoryId) => deleteCategory.mutateAsync(categoryId)}
          onClose={() => setManagingCategories(false)}
        />
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
    </div>
  )
}

interface CategoryDropZoneProps {
  categoryId: string
  children: ReactNode
}

// The whole category block (header + items) is the drop target rather than just the item list, so the
// header stays a valid place to drop on — useful when a category is down to its last line.
function CategoryDropZone({ categoryId, children }: CategoryDropZoneProps) {
  const { setNodeRef, isOver } = useDroppable({ id: categoryId })
  return (
    // -m-2 p-2 gives the highlight room around the block without moving anything when it lights up.
    <div ref={setNodeRef} className={`-m-2 rounded-2xl p-2 transition-colors ${isOver ? 'bg-accent-dim ring-2 ring-accent' : ''}`}>
      {children}
    </div>
  )
}
