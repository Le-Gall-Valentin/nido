import { useState } from 'react'
import type { DragEndEvent } from '@dnd-kit/core'
import { useMySpaces } from '@/features/space-switcher'
import { canWrite, isPersonal, useSpaceMembers } from '@/entities/space'
import {
  useTasks, useChangeTaskStatus, useToggleSubtask, useRecurringTaskSeries,
  type Task, type TaskStatus, type RecurringTaskSeries,
} from '@/entities/tasks'
import { resolveTaskMove, type TaskMove } from '../lib/resolveTaskMove'

/**
 * What the board itself needs: the tasks, who can be assigned one, what the caller may do here, and
 * which panel is open.
 *
 * <p>It used to hold everything — nine mutations, eleven slices of state, and the submit logic of every
 * form on the page — because every modal was wired from the page. Each flow now owns its own writes
 * (see the *Panel components next to this page), and what is left is what the board cannot do without:
 * moving a task between columns, which three different gestures trigger and which therefore has to be
 * decided in one place.
 */
export function useTasksPageState(spaceId: string) {
  const { data: tasks, isPending, isError } = useTasks(spaceId)
  const { data: members } = useSpaceMembers(spaceId)
  const { data: mySpaces } = useMySpaces()

  const changeTaskStatus = useChangeTaskStatus(spaceId)
  const toggleSubtask = useToggleSubtask(spaceId)
  const { data: recurringTaskSeries } = useRecurringTaskSeries(spaceId)

  const [formState, setFormState] = useState<{ mode: 'create' } | { mode: 'edit'; task: Task } | null>(null)
  const [deletingTask, setDeletingTask] = useState<Task | null>(null)
  const [movingTask, setMovingTask] = useState<Task | null>(null)
  const [statusPickerTask, setStatusPickerTask] = useState<Task | null>(null)
  const [viewingTask, setViewingTask] = useState<Task | null>(null)
  const [managingRecurringSeries, setManagingRecurringSeries] = useState(false)
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
    tasks, isPending, isError, members, recurringTaskSeries,
    canWriteHere, spaceIsPersonal, toggleSubtask,
    formState, setFormState,
    deletingTask, setDeletingTask,
    movingTask, setMovingTask,
    statusPickerTask, setStatusPickerTask,
    viewingTask, setViewingTask,
    managingRecurringSeries, setManagingRecurringSeries,
    blockedBySubtasks, dismissBlockedBySubtasks: () => setBlockedTaskId(null),
    handleToggleDone, handleDragEnd, handlePickStatus,
    seriesForTask,
  }
}
