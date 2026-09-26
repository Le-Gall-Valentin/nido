import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Plus, X } from 'lucide-react'
import { Button, Input } from '@/shared/ui'
import { newSubtaskKey } from '../lib/newSubtaskKey'

/**
 * A subtask as a form holds it. `key` names the row on screen and never changes, whatever is typed
 * into it; `id` names the subtask on the server and is absent for one that does not exist yet.
 */
export interface EditableSubtask {
  key: string
  id?: string
  text: string
}

// The column's width on the server: typing past it would only come back as a refusal.
const MAX_SUBTASK_LENGTH = 200

interface SubtaskListEditorProps {
  subtasks: EditableSubtask[]
  onChange: (subtasks: EditableSubtask[]) => void
}

/** The subtask list of a form: each subtask renamed in place or removed, a new one added below. */
export function SubtaskListEditor({ subtasks, onChange }: SubtaskListEditorProps) {
  const { t } = useTranslation('tasks')
  const [newText, setNewText] = useState('')

  function add() {
    const text = newText.trim()
    if (!text) return
    onChange([...subtasks, { key: newSubtaskKey(), text }])
    setNewText('')
  }

  return (
    <div className="flex flex-col gap-2">
      <span className="text-[13px] font-semibold text-fg-1">{t('form.subtasks_title')}</span>
      {subtasks.map((subtask, index) => (
        <div key={subtask.key} className="flex items-center gap-2">
          <div className="flex-1">
            <Input label={t('form.subtask_label', { index: index + 1 })} srOnlyLabel value={subtask.text} maxLength={MAX_SUBTASK_LENGTH}
              onChange={(e) => onChange(subtasks.map((s) => (s.key === subtask.key ? { ...s, text: e.target.value } : s)))} />
          </div>
          <button type="button" onClick={() => onChange(subtasks.filter((s) => s.key !== subtask.key))}
            aria-label={t('form.remove_subtask', { index: index + 1 })} className="p-1.5 text-fg-3 hover:text-status-red">
            <X className="size-4" />
          </button>
        </div>
      ))}
      <div className="flex items-center gap-2">
        <div className="flex-1">
          <Input label={t('form.subtask_placeholder')} srOnlyLabel placeholder={t('form.subtask_placeholder')} maxLength={MAX_SUBTASK_LENGTH}
            value={newText} onChange={(e) => setNewText(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); add() } }} />
        </div>
        <Button type="button" onClick={add} aria-label={t('form.add_subtask')}><Plus className="size-4" /></Button>
      </div>
    </div>
  )
}
