import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ConfirmDeleteModal } from '@/shared/ui'
import { useCreateCategory, useUpdateCategory, useDeleteCategory, type Category } from '@/entities/finance'
import { CategoryManagerModal } from './CategoryManagerModal'

interface CategoryManagerPanelProps {
  spaceId: string
  categories: Category[]
  onClose: () => void
}

/**
 * Managing the categories, including the one refusal that has a meaning of its own: a category still
 * used by an operation cannot be deleted, and the server says so with an error rather than a count.
 *
 * <p>That is why the delete confirmation lives here and not in a generic panel — it has to turn one
 * particular failure into one particular sentence, and it has to keep showing it while the user decides
 * what to do. The page only ever knew about it to pass it down.
 */
export function CategoryManagerPanel({ spaceId, categories, onClose }: CategoryManagerPanelProps) {
  const { t } = useTranslation('finance')
  const createCategory = useCreateCategory(spaceId)
  const updateCategory = useUpdateCategory(spaceId)
  const deleteCategory = useDeleteCategory(spaceId)

  const [deletingId, setDeletingId] = useState<string | null>(null)
  const [inUse, setInUse] = useState(false)

  const categoryById = new Map(categories.map((c) => [c.id, c]))

  return (
    <>
      <CategoryManagerModal
        categories={categories}
        onCreate={(label, color, icon, type) => createCategory.mutateAsync({ label, color, icon, type })}
        onUpdate={(categoryId, label, color, icon) => updateCategory.mutateAsync({ categoryId, label, color, icon })}
        onDelete={(categoryId) => {
          setInUse(false)
          setDeletingId(categoryId)
        }}
        onClose={onClose}
        deleteError={inUse ? 'in_use' : null}
        submitError={(createCategory.isError || updateCategory.isError) ? t('form.submit_error') : null}
      />

      {deletingId && (
        <ConfirmDeleteModal
          title={t('delete_confirm.title', { label: categoryById.get(deletingId)?.label ?? '' })}
          message={t('delete_confirm.message')}
          confirmLabel={t('delete_confirm.confirm')}
          cancelLabel={t('delete_confirm.cancel')}
          isPending={deleteCategory.isPending}
          error={inUse ? t('categories.delete_in_use') : null}
          onCancel={() => {
            setDeletingId(null)
            setInUse(false)
            deleteCategory.reset()
          }}
          onConfirm={() => deleteCategory.mutate(deletingId, {
            onSuccess: () => setDeletingId(null),
            onError: () => setInUse(true),
          })}
        />
      )}
    </>
  )
}
