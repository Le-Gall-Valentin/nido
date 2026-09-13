import { useTranslation } from 'react-i18next'
import { useDraggable } from '@dnd-kit/core'
import { Check, GripVertical, Trash2 } from 'lucide-react'
import type { ShoppingItem } from '@/entities/shopping-list'

interface ShoppingItemRowProps {
  item: ShoppingItem
  canWrite: boolean
  /** Already formatted by the page, which owns the unit vocabulary. A string compares by value. */
  quantityLabel: string | null
  onToggleDone: (itemId: string) => void
  onDelete: (itemId: string) => void
  onRequestMove: (item: ShoppingItem) => void
}

/**
 * One line of the list.
 *
 * <p><b>Deliberately not memoised</b>, and the measurement is why. Typing one character into "add an
 * item" used to re-render the page, which renders every line: thirty line renders per keystroke on a
 * thirty-item list. {@code React.memo} was tried here and changed nothing — with it in place, no prop of
 * this component changed and the lines still re-rendered, because the re-render comes from the
 * drag-and-drop context {@code useDraggable} subscribes to, and memo does not stop a context change.
 * What fixed it was moving the draft fields into {@link AddItemForm}, so the page is not re-rendered at
 * all: thirty renders per keystroke became zero.
 *
 * <p>The callbacks still take the item's id rather than closing over it, and the labels are still built
 * here: three interpolated strings per line that the page was rebuilding on every one of its renders.
 */
export function ShoppingItemRow({
  item, canWrite, quantityLabel, onToggleDone, onDelete, onRequestMove,
}: ShoppingItemRowProps) {
  const { t } = useTranslation('shopping')
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({ id: item.id })

  return (
    <div className={`flex items-center gap-2 border-b border-border px-4 py-2.5 last:border-b-0 ${isDragging ? 'opacity-40' : ''}`}>
      <button type="button" onClick={() => onToggleDone(item.id)} aria-label={t('toggle_done', { name: item.name })}
        className="grid size-5 place-items-center rounded-md border border-border">
        {item.done && <Check className="size-3.5 text-accent" />}
      </button>
      <span className={`flex-1 text-sm ${item.done ? 'text-fg-4 line-through' : 'text-fg-1'}`}>{item.name}</span>
      {quantityLabel && <span className="text-xs text-fg-3">{quantityLabel}</span>}
      {canWrite && (
        <button ref={setNodeRef} {...listeners} {...attributes} type="button" onClick={() => onRequestMove(item)}
          aria-label={t('move_item', { name: item.name })}
          className="grid size-6 shrink-0 touch-none place-items-center rounded-md text-fg-3 hover:text-fg-1 active:cursor-grabbing">
          <GripVertical className="size-4" />
        </button>
      )}
      {canWrite && (
        <button type="button" onClick={() => onDelete(item.id)} aria-label={t('delete_item', { name: item.name })}
          className="p-1 text-fg-3 hover:text-status-red">
          <Trash2 className="size-3.5" />
        </button>
      )}
    </div>
  )
}
