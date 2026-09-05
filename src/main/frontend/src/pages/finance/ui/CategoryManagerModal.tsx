import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Pencil, Trash2 } from 'lucide-react'
import { Dialog } from '@/shared/ui'
import type { Category } from '@/entities/finance'
import { resolveCategoryIcon } from '../lib/resolveCategoryIcon'

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

  return (
    <Dialog open onClose={onClose} title={t('categories.title')} maxWidth="max-w-lg">
      {deleteError && <p className="mb-2 text-sm text-red-600">{t('categories.delete_in_use')}</p>}
      <ul className="max-h-72 space-y-2 overflow-y-auto">
        {categories.map((category) => {
          const Icon = resolveCategoryIcon(category.icon)
          if (editingId === category.id) {
            return (
              <li key={category.id} className="flex items-center gap-2">
                <input type="color" value={editColor} onChange={(e) => setEditColor(e.target.value)} className="h-8 w-8" />
                <input value={editLabel} onChange={(e) => setEditLabel(e.target.value)} className="flex-1 rounded-md border px-2 py-1 text-sm" />
                <input value={editIcon} onChange={(e) => setEditIcon(e.target.value)} className="w-28 rounded-md border px-2 py-1 text-sm" />
                <button type="button" onClick={saveEdit} className="text-sm text-fg-1 underline">{t('form.save')}</button>
              </li>
            )
          }
          return (
            <li key={category.id} className="flex items-center justify-between">
              <span className="flex items-center gap-2 text-sm">
                <Icon size={16} color={category.color} />
                {category.label}
              </span>
              <span className="flex items-center gap-2">
                <button type="button" aria-label={t('categories.edit')} onClick={() => startEdit(category)}><Pencil size={16} /></button>
                <button type="button" aria-label={t('categories.delete')} onClick={() => onDelete(category.id)}><Trash2 size={16} /></button>
              </span>
            </li>
          )
        })}
      </ul>
      <div className="mt-4 flex items-center gap-2 border-t pt-4">
        <input type="color" value={newColor} onChange={(e) => setNewColor(e.target.value)} className="h-8 w-8" />
        <label className="sr-only" htmlFor="new-category-label">{t('categories.new_category_label')}</label>
        <input id="new-category-label" aria-label={t('categories.new_category_label')} placeholder={t('categories.new_category_placeholder')}
          value={newLabel} onChange={(e) => setNewLabel(e.target.value)} className="flex-1 rounded-md border px-2 py-1 text-sm" />
        <input value={newIcon} onChange={(e) => setNewIcon(e.target.value)} className="w-28 rounded-md border px-2 py-1 text-sm" />
        <button type="button" onClick={handleCreate} className="rounded-md bg-fg-1 px-3 py-1 text-sm text-bg-1">{t('categories.add')}</button>
      </div>
    </Dialog>
  )
}
