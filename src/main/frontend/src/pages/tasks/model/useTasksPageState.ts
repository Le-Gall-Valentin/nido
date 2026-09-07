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
import { resolveTaskMove } from '../lib/resolveTaskMove'

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
  const [managingRecurringSeries, setManagingRecurringSeries] = useState(false)
  const [editingSeries, setEditingSeries] = useState<RecurringTaskSeries | null>(null)
  const [deletingSeries, setDeletingSeries] = useState<RecurringTaskSeries | null>(null)

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

  function handleToggleDone(task: Task) {
    const target = task.status === 'DONE' ? 'TODO' : 'DONE'
    const resolved = resolveTaskMove(tasks ?? [], task.id, target)
    if (resolved) changeTaskStatus.mutate({ taskId: task.id, status: target })
  }

  function handleDragEnd(event: DragEndEvent) {
    if (!event.over) return
    const targetStatus = event.over.id as TaskStatus
    const resolved = resolveTaskMove(tasks ?? [], String(event.active.id), targetStatus)
    if (resolved) changeTaskStatus.mutate({ taskId: resolved.id, status: targetStatus })
  }

  async function handleMoveConfirm(destinationSpaceId: string): Promise<void> {
    if (!movingTask) return
    await moveTask.mutateAsync({ taskId: movingTask.id, destinationSpaceId })
  }

  function handlePickStatus(status: TaskStatus) {
    if (!statusPickerTask) return
    const resolved = resolveTaskMove(tasks ?? [], statusPickerTask.id, status)
    if (resolved) changeTaskStatus.mutate({ taskId: statusPickerTask.id, status })
    setStatusPickerTask(null)
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
    managingRecurringSeries, setManagingRecurringSeries,
    editingSeries, setEditingSeries,
    deletingSeries, setDeletingSeries,
    handleFormSubmit, handleUpdateSeriesSubmit, handleToggleDone, handleDragEnd, handleMoveConfirm, handlePickStatus,
  }
}
