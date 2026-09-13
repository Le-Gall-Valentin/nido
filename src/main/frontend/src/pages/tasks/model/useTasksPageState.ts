import { useState } from 'react'
import type { DragEndEvent } from '@dnd-kit/core'
import { useMySpaces, useWritableSpaces } from '@/features/space-switcher'
import { canWrite, isPersonal, useSpaceMembers } from '@/entities/space'
import {
  useTasks, useCreateTask, useCreateRecurringTask, useUpdateTask,
  useChangeTaskStatus, useToggleSubtask, useDeleteTask, useMoveTask,
  useRecurringTaskSeries, useUpdateRecurringTaskSeries, useDeleteRecurringTaskSeries,
  type Task, type TaskStatus, type RecurringTaskSeries,
} from '@/entities/tasks'
import type { TaskFormInput, RecurringTaskSeriesFormInput } from './types'
import { resolveTaskMove, type TaskMove } from '../lib/resolveTaskMove'

/**
 * Owns every query, mutation, and local UI-state slice the tasks page needs,
 * so TasksPage itself only wires the result to JSX — no data-fetching or
 * mutation orchestration left in the component.
 */
export function useTasksPageState(spaceId: string) {
  const { data: tasks, isPending, isError } = useTasks(spaceId)
  const { data: members } = useSpaceMembers(spaceId)
  const { data: mySpaces } = useMySpaces()
  const { data: writableDestinations } = useWritableSpaces(spaceId)

  const createTask = useCreateTask(spaceId)
  const createRecurringTask = useCreateRecurringTask(spaceId)
  const updateTask = useUpdateTask(spaceId)
  const changeTaskStatus = useChangeTaskStatus(spaceId)
  const toggleSubtask = useToggleSubtask(spaceId)
  const deleteTask = useDeleteTask(spaceId)
  const moveTask = useMoveTask(spaceId)
  const { data: recurringTaskSeries } = useRecurringTaskSeries(spaceId)
  const updateRecurringTaskSeries = useUpdateRecurringTaskSeries(spaceId)
  const deleteRecurringTaskSeries = useDeleteRecurringTaskSeries(spaceId)

  const [formState, setFormState] = useState<{ mode: 'create' } | { mode: 'edit'; task: Task } | null>(null)
  const [deletingTask, setDeletingTask] = useState<Task | null>(null)
  const [movingTask, setMovingTask] = useState<Task | null>(null)
  const [statusPickerTask, setStatusPickerTask] = useState<Task | null>(null)
  const [viewingTask, setViewingTask] = useState<Task | null>(null)
  const [managingRecurringSeries, setManagingRecurringSeries] = useState(false)
  const [editingSeries, setEditingSeries] = useState<RecurringTaskSeries | null>(null)
  const [deletingSeries, setDeletingSeries] = useState<RecurringTaskSeries | null>(null)
  const [viewingSeries, setViewingSeries] = useState<RecurringTaskSeries | null>(null)
  const [blockedTaskId, setBlockedTaskId] = useState<string | null>(null)

  /**
   * Why the id and not the message: the reason is re-read from the tasks themselves on every render,
   * so finishing the last subtask takes the warning down on its own. Kept as text, it would outlive
   * what it was warning about.
   */
  const blockedTask = blockedTaskId ? tasks?.find((t) => t.id === blockedTaskId) : undefined
  const openSubtasks = blockedTask?.subtasks.filter((sub) => !sub.done).length ?? 0
  const blockedBySubtasks = blockedTask && openSubtasks > 0
    ? { title: blockedTask.title, openSubtasks }
    : null

  const currentSpace = mySpaces?.find((s) => s.id === spaceId)
  const canWriteHere = currentSpace ? canWrite(currentSpace.myRole) : false
  const spaceIsPersonal = currentSpace ? isPersonal(currentSpace) : false

  function closeForm() {
    setFormState(null)
    createTask.reset()
    createRecurringTask.reset()
    updateTask.reset()
  }

  function handleFormSubmit(input: TaskFormInput) {
    if (formState?.mode === 'edit') {
      updateTask.mutate(
        { taskId: formState.task.id, title: input.title, priority: input.priority, dueDate: input.dueDate, assigneeIds: input.assigneeIds },
        { onSuccess: () => setFormState(null) }
      )
      return
    }
    if (input.recurrence) {
      createRecurringTask.mutate(
        { title: input.title, priority: input.priority, subtasks: input.subtasks, recurrence: input.recurrence },
        { onSuccess: () => setFormState(null) }
      )
      return
    }
    createTask.mutate(
      { title: input.title, priority: input.priority, dueDate: input.dueDate, assigneeIds: input.assigneeIds, subtasks: input.subtasks },
      { onSuccess: () => setFormState(null) }
    )
  }

  function handleUpdateSeriesSubmit(input: RecurringTaskSeriesFormInput) {
    if (!editingSeries) return
    updateRecurringTaskSeries.mutate({ seriesId: editingSeries.id, ...input }, { onSuccess: () => setEditingSeries(null) })
  }

  /**
   * The one place a resolved move is acted on, so that a refusal is reported wherever it comes from:
   * the card's checkbox, a drag onto the column, or the status dialog.
   */
  function applyMove(move: TaskMove, targetStatus: TaskStatus) {
    if (move.kind === 'blocked') {
      setBlockedTaskId(move.task.id)
      return
    }
    if (move.kind === 'move') {
      setBlockedTaskId(null)
      changeTaskStatus.mutate({ taskId: move.task.id, status: targetStatus })
    }
  }

  function handleToggleDone(task: Task) {
    const target = task.status === 'DONE' ? 'TODO' : 'DONE'
    applyMove(resolveTaskMove(tasks ?? [], task.id, target), target)
  }

  function handleDragEnd(event: DragEndEvent) {
    if (!event.over) return
    const targetStatus = event.over.id as TaskStatus
    applyMove(resolveTaskMove(tasks ?? [], String(event.active.id), targetStatus), targetStatus)
  }

  async function handleMoveConfirm(destinationSpaceId: string): Promise<void> {
    if (!movingTask) return
    await moveTask.mutateAsync({ taskId: movingTask.id, destinationSpaceId })
  }

  function handlePickStatus(status: TaskStatus) {
    if (!statusPickerTask) return
    // The dialog already disables a blocked target, so this goes through applyMove for consistency
    // rather than to be seen — and stays correct if that dialog ever stops disabling it.
    applyMove(resolveTaskMove(tasks ?? [], statusPickerTask.id, status), status)
    setStatusPickerTask(null)
  }

  /** The series a materialized occurrence belongs to — null for a one-off task,
   * or a recurring one whose series has since been deleted. */
  function seriesForTask(task: Task): RecurringTaskSeries | null {
    if (!task.recurringSeriesId) return null
    return (recurringTaskSeries ?? []).find((s) => s.id === task.recurringSeriesId) ?? null
  }

  return {
    tasks, isPending, isError, members, writableDestinations, recurringTaskSeries,
    canWriteHere, spaceIsPersonal,
    createTask, createRecurringTask, updateTask, toggleSubtask, deleteTask,
    updateRecurringTaskSeries, deleteRecurringTaskSeries,
    formState, setFormState, closeForm,
    deletingTask, setDeletingTask,
    movingTask, setMovingTask,
    statusPickerTask, setStatusPickerTask,
    viewingTask, setViewingTask,
    managingRecurringSeries, setManagingRecurringSeries,
    editingSeries, setEditingSeries,
    deletingSeries, setDeletingSeries,
    viewingSeries, setViewingSeries,
    blockedBySubtasks, dismissBlockedBySubtasks: () => setBlockedTaskId(null),
    handleFormSubmit, handleUpdateSeriesSubmit, handleToggleDone, handleDragEnd, handleMoveConfirm, handlePickStatus,
    seriesForTask,
  }
}
