import { useDeleteTask, type Task } from '@/entities/tasks'
import { DeleteTaskModal } from './DeleteTaskModal'

interface DeleteTaskPanelProps {
  spaceId: string
  task: Task
  onClose: () => void
}

/**
 * The confirmation and the call it makes, kept together. The modal owns what the user sees while it
 * runs — pending, failed — and needs a promise to await; this is where that promise comes from.
 */
export function DeleteTaskPanel({ spaceId, task, onClose }: DeleteTaskPanelProps) {
  const deleteTask = useDeleteTask(spaceId)

  return (
    <DeleteTaskModal
      taskTitle={task.title}
      onClose={onClose}
      onDelete={() => deleteTask.mutateAsync(task.id)}
    />
  )
}
