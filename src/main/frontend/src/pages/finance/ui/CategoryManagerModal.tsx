import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Pencil, Trash2 } from 'lucide-react'
import { Dialog, Button, Input } from '@/shared/ui'
import type { Category } from '@/entities/finance'
import { CategoryIconBadge } from './CategoryIconBadge'
import { IconPickerModal } from './IconPickerModal'

interface CategoryManagerModalProps {
  categories: Category[]
  onCreate: (label: string, color: string, icon: string) => void
  onUpdate: (categoryId: string, label: string, color: string, icon: string) => void
  onDelete: (categoryId: string) => void
  onClose: () => void
  deleteError: string | null
}

const DEFAULT_NEW_COLOR = '#64748b'
const DEFAULT_NEW_ICON = 'Circle'

export function CategoryManagerModal({ categories, onCreate, onUpdate, onDelete, onClose, deleteError }: CategoryManagerModalProps) {
  const { t } = useTranslation('finance')
  const [newLabel, setNewLabel] = useState('')
  const [newColor, setNewColor] = useState(DEFAULT_NEW_COLOR)
  const [newIcon, setNewIcon] = useState(DEFAULT_NEW_ICON)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [editLabel, setEditLabel] = useState('')
  const [editColor, setEditColor] = useState('')
  const [editIcon, setEditIcon] = useState('')
  const [pickerTarget, setPickerTarget] = useState<'new' | 'edit' | null>(null)

  function startEdit(category: Category) {
    setEditingId(category.id)
    setEditLabel(category.label)
    setEditColor(category.color)
    setEditIcon(category.icon)
  }

  function saveEdit() {
    if (editingId) onUpdate(editingId, editLabel, editColor, editIcon)
    setEditingId(null)
  }

  function handleCreate() {
    if (!newLabel.trim()) return
    onCreate(newLabel.trim(), newColor, newIcon)
    setNewLabel('')
    setNewColor(DEFAULT_NEW_COLOR)
    setNewIcon(DEFAULT_NEW_ICON)
  }

  function handleAppearanceConfirm(icon: string, color: string) {
    if (pickerTarget === 'new') {
      setNewIcon(icon)
      setNewColor(color)
    } else if (pickerTarget === 'edit') {
      setEditIcon(icon)
      setEditColor(color)
    }
    setPickerTarget(null)
  }

  return (
    <Dialog open onClose={onClose} title={t('categories.title')} maxWidth="max-w-lg">
      <h3 className="mb-4 text-[19px] font-semibold text-fg-0">{t('categories.title')}</h3>

      {deleteError && <p className="mb-3 text-sm font-medium text-status-red">{t('categories.delete_in_use')}</p>}

      <ul className="max-h-72 space-y-1 overflow-y-auto">
        {categories.map((category) => {
          if (editingId === category.id) {
            return (
              <li key={category.id} className="flex items-center gap-2 rounded-[10px] bg-bg-2 p-2">
                <button type="button" aria-label={t('categories.edit_appearance')} onClick={() => setPickerTarget('edit')}>
                  <CategoryIconBadge category={{ color: editColor, icon: editIcon }} size={32} />
                </button>
                <input value={editLabel} onChange={(e) => setEditLabel(e.target.value)}
                  className="flex-1 rounded-[8px] border-[1.5px] border-border bg-bg-1 px-2.5 py-1.5 text-sm text-fg-0 outline-none focus:border-accent" />
                <button type="button" onClick={saveEdit} className="text-sm font-semibold text-accent">{t('form.save')}</button>
              </li>
            )
          }
          return (
            <li key={category.id} className="flex items-center gap-3 rounded-[10px] p-2 hover:bg-bg-2">
              <CategoryIconBadge category={category} size={32} />
              <span className="flex-1 truncate text-sm font-medium text-fg-0">{category.label}</span>
              <button type="button" aria-label={t('categories.edit')} onClick={() => startEdit(category)}
                className="grid size-7 place-items-center rounded-md text-fg-3 hover:bg-bg-1 hover:text-fg-1">
                <Pencil size={15} />
              </button>
              <button type="button" aria-label={t('categories.delete')} onClick={() => onDelete(category.id)}
                className="grid size-7 place-items-center rounded-md text-fg-3 hover:bg-status-red-dim hover:text-status-red">
                <Trash2 size={15} />
              </button>
            </li>
          )
        })}
      </ul>

      <div className="mt-4 flex items-end gap-2 border-t border-border pt-4">
        <button type="button" aria-label={t('categories.choose_appearance')} onClick={() => setPickerTarget('new')}>
          <CategoryIconBadge category={{ color: newColor, icon: newIcon }} size={42} />
        </button>
        <Input label={t('categories.new_category_label')} srOnlyLabel placeholder={t('categories.new_category_placeholder')}
          value={newLabel} onChange={(e) => setNewLabel(e.target.value)} className="flex-1" />
        <Button type="button" onClick={handleCreate}>{t('categories.add')}</Button>
      </div>

      {pickerTarget && (
        <IconPickerModal
          initialIcon={pickerTarget === 'new' ? newIcon : editIcon}
          initialColor={pickerTarget === 'new' ? newColor : editColor}
          onConfirm={handleAppearanceConfirm}
          onCancel={() => setPickerTarget(null)}
        />
      )}
    </Dialog>
  )
}
