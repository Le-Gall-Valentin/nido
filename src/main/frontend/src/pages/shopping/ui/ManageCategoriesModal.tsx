import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Check, Pencil, Plus, Trash2, X } from 'lucide-react'
import { Alert, Button, Dialog, Input } from '@/shared/ui'
import type { ShoppingCategory } from '@/entities/shopping-list'

interface ManageCategoriesModalProps {
  categories: ShoppingCategory[]
  /** Per category, how many items it holds — the deletion notice counts what moves to the fallback. */
  itemCountByCategory: Map<string, number>
  onCreate: (name: string) => Promise<unknown>
  onRename: (categoryId: string, name: string) => Promise<unknown>
  onDelete: (categoryId: string) => Promise<unknown>
  onClose: () => void
}

/**
 * Where categories are renamed, deleted and created — the only place, now.
 *
 * <p>The list itself used to carry a "Rename" button and a bin on every category header: twelve
 * always-visible controls on a six-category list, competing for attention with the items, for an action
 * taken about once a year. The headers are back to a name and a count, and the whole vocabulary of
 * managing categories lives here.
 *
 * <p>Deleting is confirmed **in place** rather than through the shared {@code ConfirmDeleteModal}: two
 * stacked dialogs mean two competing focus traps. The notice can also be more useful than the generic
 * "this cannot be undone" — the backend reassigns the category's items to the fallback one before
 * deleting it, so nothing is lost, and the row says so with the real count.
 */
export function ManageCategoriesModal({
  categories, itemCountByCategory, onCreate, onRename, onDelete, onClose,
}: ManageCategoriesModalProps) {
  const { t } = useTranslation('shopping')

  const [newName, setNewName] = useState('')
  const [renamingId, setRenamingId] = useState<string | null>(null)
  const [renameDraft, setRenameDraft] = useState('')
  const [deletingId, setDeletingId] = useState<string | null>(null)
  const [failed, setFailed] = useState(false)
  const [pending, setPending] = useState(false)

  const fallbackName = categories.find((c) => c.fallback)?.name ?? ''

  function run(promise: Promise<unknown>, onDone: () => void) {
    setFailed(false)
    setPending(true)
    promise
      .then(() => { onDone(); setPending(false) })
      .catch(() => { setFailed(true); setPending(false) })
  }

  function create() {
    const name = newName.trim()
    if (!name || pending) return
    run(onCreate(name), () => setNewName(''))
  }

  function confirmRename() {
    const name = renameDraft.trim()
    if (!renamingId || !name || pending) return
    run(onRename(renamingId, name), () => setRenamingId(null))
  }

  function startRename(category: ShoppingCategory) {
    setDeletingId(null)
    setRenamingId(category.id)
    setRenameDraft(category.name)
  }

  function confirmDelete(categoryId: string) {
    if (pending) return
    run(onDelete(categoryId), () => setDeletingId(null))
  }

  return (
    <Dialog open onClose={onClose} title={t('manage_categories_title')}>
      <h3 className="mb-1 text-[19px] font-semibold text-fg-0">{t('manage_categories_title')}</h3>
      <p className="mb-4 text-xs text-fg-3">{t('manage_categories_subtitle')}</p>

      {failed && <Alert variant="error" className="mb-3">{t('error.action_failed')}</Alert>}

      <ul className="mb-5 flex flex-col divide-y divide-border border-y border-border">
        {categories.map((category) => (
          <li key={category.id} className="py-2.5">
            {renamingId === category.id ? (
              <div className="flex items-center gap-2">
                <div className="flex-1">
                  <Input
                    label={t('category_rename')} srOnlyLabel autoFocus value={renameDraft}
                    onChange={(e) => setRenameDraft(e.target.value)}
                    onKeyDown={(e) => { if (e.key === 'Enter') confirmRename() }}
                    className="py-1.5 text-sm"
                  />
                </div>
                <button type="button" onClick={confirmRename} aria-label={t('category_rename_confirm')}
                  className="grid size-7 place-items-center rounded-md text-accent hover:bg-bg-2">
                  <Check className="size-4" />
                </button>
                <button type="button" onClick={() => setRenamingId(null)} aria-label={t('form_cancel')}
                  className="grid size-7 place-items-center rounded-md text-fg-3 hover:bg-bg-2">
                  <X className="size-4" />
                </button>
              </div>
            ) : (
              <div className="flex items-center gap-2">
                {/* What the category *is* travels with its name; what it *holds* keeps the column on
                    the right, for every row alike. Wrapping rather than truncating: on a narrow
                    screen a long name pushes the badge onto a second line instead of eating it. */}
                <span className="flex min-w-0 flex-1 flex-wrap items-center gap-x-2 gap-y-1">
                  <span className="text-sm text-fg-1">{category.name}</span>
                  {category.fallback && (
                    <span className="rounded-full bg-bg-2 px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-fg-3">
                      {t('category_fallback_hint')}
                    </span>
                  )}
                </span>
                <span className="shrink-0 text-xs text-fg-4">
                  {t('category_item_count', { count: itemCountByCategory.get(category.id) ?? 0 })}
                </span>
                <button type="button" onClick={() => startRename(category)}
                  aria-label={t('category_rename_for', { name: category.name })}
                  className="grid size-7 place-items-center rounded-md text-fg-3 hover:bg-bg-2 hover:text-fg-1">
                  <Pencil className="size-3.5" />
                </button>
                {category.fallback ? (
                  // The fallback row has no bin, and without this the pencils would not line up: its
                  // own would sit where every other row shows a bin.
                  <span aria-hidden="true" className="size-7 shrink-0" />
                ) : (
                  <button type="button" onClick={() => { setRenamingId(null); setDeletingId(category.id) }}
                    aria-label={t('category_delete', { name: category.name })}
                    className="grid size-7 place-items-center rounded-md text-fg-3 hover:bg-bg-2 hover:text-status-red">
                    <Trash2 className="size-3.5" />
                  </button>
                )}
              </div>
            )}

            {deletingId === category.id && (
              <div className="mt-2 rounded-[10px] bg-status-red-dim px-3 py-2.5">
                <p className="text-sm font-semibold text-fg-1">{t('category_delete_confirm', { name: category.name })}</p>
                <p className="mt-0.5 text-xs text-fg-2">
                  {t('category_delete_reassign', { count: itemCountByCategory.get(category.id) ?? 0, fallback: fallbackName })}
                </p>
                <div className="mt-2.5 flex justify-end gap-2">
                  <button type="button" onClick={() => setDeletingId(null)} disabled={pending}
                    className="rounded-md px-2.5 py-1.5 text-xs font-semibold text-fg-2 hover:bg-bg-2 disabled:opacity-50">
                    {t('form_cancel')}
                  </button>
                  <button type="button" onClick={() => confirmDelete(category.id)} disabled={pending}
                    className="rounded-md bg-status-red px-2.5 py-1.5 text-xs font-semibold text-bg-0 disabled:opacity-50">
                    {t('category_delete_confirm_action')}
                  </button>
                </div>
              </div>
            )}
          </li>
        ))}
      </ul>

      <div className="mb-5 flex items-end gap-2">
        <div className="flex-1">
          <Input
            label={t('new_category_placeholder')} srOnlyLabel value={newName}
            onChange={(e) => setNewName(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter') create() }}
            placeholder={t('new_category_placeholder')}
          />
        </div>
        <button type="button" onClick={create} disabled={pending}
          className="flex shrink-0 items-center gap-1.5 rounded-[10px] border-[1.5px] border-dashed border-border-2 px-3 py-[11px] text-xs font-semibold text-fg-2 hover:bg-bg-2 hover:text-fg-0 disabled:opacity-50">
          <Plus className="size-3.5" /> {t('new_category')}
        </button>
      </div>

      <div className="flex justify-end">
        <Button type="button" onClick={onClose}>{t('form_close')}</Button>
      </div>
    </Dialog>
  )
}
