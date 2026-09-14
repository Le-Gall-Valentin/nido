import { useWritableSpaces } from '@/features/space-switcher'
import { useMoveTask, type Task } from '@/entities/tasks'
import { TransferDialog } from '@/features/transfer-to-space'

interface MoveTaskPanelProps {
  spaceId: string
  task: Task
  onClose: () => void
}

/**
 * Sending a task to another context: the destinations the caller may write to, and the call that moves
 * it. Both are asked for only once this is open, which is also the only time either means anything —
 * the board itself has no use for the list of other contexts.
 */
export function MoveTaskPanel({ spaceId, task, onClose }: MoveTaskPanelProps) {
  const { data: destinations } = useWritableSpaces(spaceId)
  const moveTask = useMoveTask(spaceId)

  return (
    <TransferDialog
      itemName={task.title}
      operation="move"
      destinations={destinations ?? []}
      onClose={onClose}
      onConfirm={async (destinationSpaceId) => {
        await moveTask.mutateAsync({ taskId: task.id, destinationSpaceId })
      }}
    />
  )
}
