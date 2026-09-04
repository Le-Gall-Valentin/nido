import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Plus, Pencil, ArrowRightLeft, Repeat } from 'lucide-react'
import {
  DndContext, useDraggable, useDroppable, PointerSensor, useSensor, useSensors, type DragEndEvent,
} from '@dnd-kit/core'
import { Alert, Dialog, Spinner } from '@/shared/ui'
import { useMySpaces, useWritableSpaces } from '@/features/space-switcher'
import { canWrite, isPersonal, useSpaceMembers, TransferDialog } from '@/entities/space'
import { UserAvatar } from '@/entities/user'
import {
  tasksApi, TasksApiProvider, useTasks, useCreateTask, useCreateRecurringTask, useUpdateTask,
  useChangeTaskStatus, useToggleSubtask, useDeleteTask, useMoveTask,
  type ITasksApi, type Task, type TaskStatus,
} from '@/entities/tasks'
import { TASK_PRIORITY_META } from '../lib/taskPriorityMeta'
import { TaskFormModal, type TaskFormInput } from './TaskFormModal'
import { DeleteTaskModal } from './DeleteTaskModal'
import { resolveTaskMove } from './resolveTaskMove'

const COLUMN_ORDER: TaskStatus[] = ['TODO', 'DOING', 'DONE']

interface TasksPageProps {
  api?: ITasksApi
}

export function TasksPage({ api = tasksApi }: TasksPageProps = {}) {
  return (
    <TasksApiProvider api={api}>
      <TasksPageContent />
    </TasksApiProvider>
  )
}

function isOverdue(dueDate: string | null): boolean {
  if (!dueDate) return false
  return dueDate < new Date().toISOString().slice(0, 10)
}

interface TaskCardProps {
  task: Task
  members: ReturnType<typeof useSpaceMembers>['data']
  canWriteHere: boolean
  onToggleDone: (task: Task) => void
  onToggleSubtask: (taskId: string, subtaskId: string) => void
  onEdit: (task: Task) => void
  onMove: (task: Task) => void
  onDelete: (task: Task) => void
  onChangeStatus: (task: Task) => void
}

function TaskCard({ task, members, canWriteHere, onToggleDone, onToggleSubtask, onEdit, onMove, onDelete, onChangeStatus }: TaskCardProps) {
  const { t } = useTranslation('tasks')
  const { attributes, listeners, setNodeRef, transform } = useDraggable({ id: task.id })
  const meta = TASK_PRIORITY_META[task.priority]
  const doneSubtasks = task.subtasks.filter((s) => s.done).length
  const changeStatusLabel = t('change_status', { title: task.title })

  const priorityRowContent = (
    <>
      <span className={`flex items-center gap-1 font-semibold ${meta.textClassName}`}>
        <span className={`size-1.5 rounded-full ${meta.dotClassName}`} />
        {t(meta.labelKey)}
      </span>
      {task.dueDate && <span className={isOverdue(task.dueDate) ? 'text-status-red' : 'text-fg-4'}>{task.dueDate}</span>}
      {task.assigneeIds.length > 0 && (
        <div className="ml-auto flex -space-x-1.5">
          {task.assigneeIds.map((userId) => {
            const member = members?.find((m) => m.userId === userId)
            return <UserAvatar key={userId} username={member?.username ?? '?'} role="USER" className="size-6 rounded-full border-2 border-bg-1 text-[10px]" />
          })}
        </div>
      )}
    </>
  )

  return (
    <div
      ref={setNodeRef}
      {...listeners}
      {...attributes}
      style={transform ? { transform: `translate3d(${transform.x}px, ${transform.y}px, 0)` } : undefined}
      className="touch-none flex flex-col gap-2 rounded-2xl border border-border bg-bg-1 p-3"
    >
      <div className="flex items-start gap-2.5">
        <button type="button" aria-label={t('toggle_done', { title: task.title })} onClick={() => onToggleDone(task)}
          className={`mt-0.5 grid size-5 shrink-0 place-items-center rounded-[6px] border-2 ${task.status === 'DONE' ? 'border-status-green bg-status-green' : 'border-border'}`}>
          {task.status === 'DONE' && <span className="text-[10px] font-bold text-white">✓</span>}
        </button>
        {canWriteHere ? (
          <button type="button" onClick={() => onChangeStatus(task)} aria-label={changeStatusLabel}
            className={`flex-1 text-left text-sm ${task.status === 'DONE' ? 'text-fg-3 line-through' : 'text-fg-0'}`}>
            {task.title}
          </button>
        ) : (
          <span className={`flex-1 text-sm ${task.status === 'DONE' ? 'text-fg-3 line-through' : 'text-fg-0'}`}>{task.title}</span>
        )}
        {task.recurring && <Repeat className="mt-0.5 size-3.5 shrink-0 text-fg-3" />}
      </div>

      {task.subtasks.length > 0 && (
        <div className="flex flex-col gap-1 pl-[30px]">
          <div className="text-[10.5px] font-semibold text-fg-4">{doneSubtasks}/{task.subtasks.length}</div>
          {task.subtasks.map((subtask) => (
            <button key={subtask.id} type="button" onClick={() => onToggleSubtask(task.id, subtask.id)}
              className="flex items-center gap-2 text-left">
              <span className={`grid size-3.5 shrink-0 place-items-center rounded-[4px] border ${subtask.done ? 'border-status-green bg-status-green' : 'border-border'}`}>
                {subtask.done && <span className="text-[8px] font-bold text-white">✓</span>}
              </span>
              <span className={`text-xs ${subtask.done ? 'text-fg-4 line-through' : 'text-fg-2'}`}>{subtask.text}</span>
            </button>
          ))}
        </div>
      )}

      {canWriteHere ? (
        <button type="button" onClick={() => onChangeStatus(task)} aria-label={changeStatusLabel}
          className="flex w-full items-center gap-2 pl-[30px] text-left text-[11.5px]">
          {priorityRowContent}
        </button>
      ) : (
        <div className="flex items-center gap-2 pl-[30px] text-[11.5px]">{priorityRowContent}</div>
      )}

      {canWriteHere && (
        <div className="flex gap-1 border-t border-border pt-2">
          <button type="button" onClick={() => onEdit(task)} className="flex flex-1 items-center justify-center gap-1 rounded-[8px] py-1 text-xs font-semibold text-fg-2 hover:bg-bg-2">
            <Pencil className="size-3.5" /> {t('edit')}
          </button>
          <button type="button" onClick={() => onMove(task)} className="flex flex-1 items-center justify-center gap-1 rounded-[8px] py-1 text-xs font-semibold text-fg-2 hover:bg-bg-2">
            <ArrowRightLeft className="size-3.5" /> {t('move')}
          </button>
          <button type="button" onClick={() => onDelete(task)} className="flex-1 rounded-[8px] py-1 text-xs font-semibold text-status-red hover:bg-status-red-dim">
            {t('delete')}
          </button>
        </div>
      )}
    </div>
  )
}

function TaskColumn({ status, tasks, ...cardProps }: { status: TaskStatus; tasks: Task[] } & Omit<TaskCardProps, 'task'>) {
  const { t } = useTranslation('tasks')
  const { setNodeRef } = useDroppable({ id: status })
  return (
    <div ref={setNodeRef} className="flex flex-col gap-2.5 rounded-2xl bg-bg-2 p-3">
      <div className="flex items-center gap-2 px-1 pb-1 text-sm font-semibold text-fg-2">
        {t(`column.${status}`)} <span className="ml-auto text-xs text-fg-4">{tasks.length}</span>
      </div>
      {tasks.map((task) => <TaskCard key={task.id} task={task} {...cardProps} />)}
      {tasks.length === 0 && (
        <div className="rounded-[10px] border border-dashed border-border p-3 text-center text-xs text-fg-4">{t('drop_here')}</div>
      )}
    </div>
  )
}

function TasksPageContent() {
  const { t } = useTranslation('tasks')
  const { spaceId = '' } = useParams<{ spaceId: string }>()
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

  const [formState, setFormState] = useState<{ mode: 'create' } | { mode: 'edit'; task: Task } | null>(null)
  const [deletingTask, setDeletingTask] = useState<Task | null>(null)
  const [movingTask, setMovingTask] = useState<Task | null>(null)
  const [statusPickerTask, setStatusPickerTask] = useState<Task | null>(null)

  const currentSpace = mySpaces?.find((s) => s.id === spaceId)
  const canWriteHere = currentSpace ? canWrite(currentSpace.myRole) : false
  const spaceIsPersonal = currentSpace ? isPersonal(currentSpace) : false

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 8 } }))

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

  if (isPending) return <Spinner label={t('loading')} fullscreen={false} />
  if (isError) return <Alert variant="error">{t('error.load_failed')}</Alert>

  const cardProps = {
    members, canWriteHere,
    onToggleDone: handleToggleDone,
    onToggleSubtask: (taskId: string, subtaskId: string) => toggleSubtask.mutate({ taskId, subtaskId }),
    onEdit: (task: Task) => setFormState({ mode: 'edit', task }),
    onMove: (task: Task) => setMovingTask(task),
    onDelete: (task: Task) => setDeletingTask(task),
    onChangeStatus: (task: Task) => setStatusPickerTask(task),
  }

  return (
    <div className="mx-auto max-w-[1100px] px-5 py-6 md:px-10 md:py-[34px]">
      <div className="mb-5 flex items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-fg-0">{t('title')}</h1>
        {canWriteHere && (
          <button type="button" onClick={() => setFormState({ mode: 'create' })}
            className="flex items-center gap-1.5 rounded-[10px] bg-accent px-4 py-2.5 text-sm font-semibold text-white">
            <Plus className="size-4" /> {t('new_task')}
          </button>
        )}
      </div>

      <DndContext sensors={sensors} onDragEnd={handleDragEnd}>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          {COLUMN_ORDER.map((status) => (
            <TaskColumn key={status} status={status} tasks={(tasks ?? []).filter((task) => task.status === status)} {...cardProps} />
          ))}
        </div>
      </DndContext>

      {formState && (
        <TaskFormModal
          open
          onClose={() => setFormState(null)}
          onSubmit={handleFormSubmit}
          initialTask={formState.mode === 'edit' ? formState.task : null}
          members={members ?? []}
          isPersonal={spaceIsPersonal}
        />
      )}

      {deletingTask && (
        <DeleteTaskModal
          taskTitle={deletingTask.title}
          onClose={() => setDeletingTask(null)}
          onDelete={() => deleteTask.mutateAsync(deletingTask.id)}
        />
      )}

      {movingTask && (
        <TransferDialog
          itemName={movingTask.title}
          operation="move"
          destinations={writableDestinations ?? []}
          onClose={() => setMovingTask(null)}
          onConfirm={handleMoveConfirm}
        />
      )}

      {statusPickerTask && (
        <Dialog open onClose={() => setStatusPickerTask(null)} title={t('change_status_dialog_title', { title: statusPickerTask.title })}>
          <p className="mb-3 text-sm font-semibold text-fg-1">{t('change_status_dialog_title', { title: statusPickerTask.title })}</p>
          <div className="flex flex-col gap-1">
            {COLUMN_ORDER.map((status) => {
              const isCurrent = status === statusPickerTask.status
              const blockedBySubtasks = status === 'DONE' && statusPickerTask.subtasks.some((s) => !s.done)
              const label = isCurrent
                ? t('change_status_target_current', { column: t(`column.${status}`) })
                : blockedBySubtasks
                  ? t('change_status_target_blocked', { column: t(`column.${status}`) })
                  : t(`column.${status}`)
              return (
                <button key={status} type="button" disabled={isCurrent || blockedBySubtasks} onClick={() => handlePickStatus(status)}
                  className="rounded-md px-3 py-2 text-left text-sm hover:bg-bg-2 disabled:text-fg-4 disabled:hover:bg-transparent">
                  {label}
                </button>
              )
            })}
          </div>
        </Dialog>
      )}
    </div>
  )
}
