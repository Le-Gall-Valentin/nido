import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { GripVertical, Plus, X } from 'lucide-react'
import { DndContext, closestCenter, type DragEndEvent, type Modifier } from '@dnd-kit/core'
import { SortableContext, arrayMove, sortableKeyboardCoordinates, useSortable, verticalListSortingStrategy } from '@dnd-kit/sortable'
import { Button, Input } from '@/shared/ui'
import { useDragSensors } from '@/shared/lib'
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

// A list reorders up and down: a row wandering sideways under the pointer shows nothing more.
const alongTheList: Modifier = ({ transform }) => ({ ...transform, x: 0 })

interface SubtaskListEditorProps {
  subtasks: EditableSubtask[]
  onChange: (subtasks: EditableSubtask[]) => void
}

/**
 * The subtask list of a form: each subtask renamed in place, removed, or dragged by its handle to
 * another place in the list; a new one added below. The drag starts from the handle and not the
 * row, because pressing in the row's field has to put the caret there.
 */
export function SubtaskListEditor({ subtasks, onChange }: SubtaskListEditorProps) {
  const { t } = useTranslation('tasks')
  const [newText, setNewText] = useState('')
  const sensors = useDragSensors({ keyboardCoordinates: sortableKeyboardCoordinates })

  function add() {
    const text = newText.trim()
    if (!text) return
    onChange([...subtasks, { key: newSubtaskKey(), text }])
    setNewText('')
  }

  function handleDragEnd({ active, over }: DragEndEvent) {
    if (!over || active.id === over.id) return
    const from = subtasks.findIndex((s) => s.key === active.id)
    const to = subtasks.findIndex((s) => s.key === over.id)
    onChange(arrayMove(subtasks, from, to))
  }

  const textOf = (key: unknown) => subtasks.find((s) => s.key === key)?.text ?? ''
  const positionOf = (key: unknown) => subtasks.findIndex((s) => s.key === key) + 1

  return (
    <div className="flex flex-col gap-2">
      <span className="text-[13px] font-semibold text-fg-1">{t('form.subtasks_title')}</span>
      <DndContext sensors={sensors} collisionDetection={closestCenter} modifiers={[alongTheList]} onDragEnd={handleDragEnd}
        accessibility={{
          screenReaderInstructions: { draggable: t('form.move_subtask_instructions') },
          announcements: {
            onDragStart: ({ active }) => t('form.move_subtask_picked', { text: textOf(active.id) }),
            onDragOver: ({ active, over }) => (over ? t('form.move_subtask_over', { text: textOf(active.id), position: positionOf(over.id) }) : undefined),
            onDragEnd: ({ active, over }) => (over ? t('form.move_subtask_dropped', { text: textOf(active.id), position: positionOf(over.id) }) : undefined),
            onDragCancel: ({ active }) => t('form.move_subtask_cancelled', { text: textOf(active.id) }),
          },
        }}>
        <SortableContext items={subtasks.map((s) => s.key)} strategy={verticalListSortingStrategy}>
          <div className="flex flex-col gap-2">
            {subtasks.map((subtask, index) => (
              <SubtaskRow key={subtask.key} subtask={subtask} position={index + 1}
                onRename={(text) => onChange(subtasks.map((s) => (s.key === subtask.key ? { ...s, text } : s)))}
                onRemove={() => onChange(subtasks.filter((s) => s.key !== subtask.key))} />
            ))}
          </div>
        </SortableContext>
      </DndContext>
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

interface SubtaskRowProps {
  subtask: EditableSubtask
  position: number
  onRename: (text: string) => void
  onRemove: () => void
}

function SubtaskRow({ subtask, position, onRename, onRemove }: SubtaskRowProps) {
  const { t } = useTranslation('tasks')
  const { attributes, listeners, setNodeRef, setActivatorNodeRef, transform, transition, isDragging } = useSortable({ id: subtask.key })

  return (
    <div ref={setNodeRef} data-subtask-row
      style={{ transform: transform ? `translate3d(${transform.x}px, ${transform.y}px, 0)` : undefined, transition }}
      className={`flex items-center gap-2 rounded-[10px] ${isDragging ? 'relative z-10 bg-bg-1 shadow-lg' : ''}`}>
      <button ref={setActivatorNodeRef} type="button" {...attributes} {...listeners}
        aria-label={t('form.move_subtask', { index: position })}
        // touch-manipulation, never touch-none: a finger swiping across the handle still scrolls the
        // form. TouchSensor blocks the scroll itself, once the long press has started a drag.
        className="grid size-8 shrink-0 cursor-grab touch-manipulation select-none place-items-center rounded-md text-fg-3 hover:bg-bg-2 hover:text-fg-1 active:cursor-grabbing">
        <GripVertical className="size-4" />
      </button>
      <div className="flex-1">
        <Input label={t('form.subtask_label', { index: position })} srOnlyLabel value={subtask.text} maxLength={MAX_SUBTASK_LENGTH}
          onChange={(e) => onRename(e.target.value)} />
      </div>
      <button type="button" onClick={onRemove} aria-label={t('form.remove_subtask', { index: position })}
        className="p-1.5 text-fg-3 hover:text-status-red">
        <X className="size-4" />
      </button>
    </div>
  )
}
