import { describe, it, expect } from 'vitest'
import { resolveTaskMove } from './resolveTaskMove'
import type { Task } from '@/entities/tasks'

const TASKS: Task[] = [
  { id: 't1', title: 'À faire', status: 'TODO', priority: 'MED', dueDate: null, assigneeIds: [], subtasks: [], recurring: false, recurringSeriesId: null, createdBy: null },
  {
    id: 't2', title: 'Sous-tâches ouvertes', status: 'DOING', priority: 'MED', dueDate: null, assigneeIds: [], recurring: false, recurringSeriesId: null, createdBy: null,
    subtasks: [{ id: 's1', text: 'A', done: true }, { id: 's2', text: 'B', done: false }, { id: 's3', text: 'C', done: false }],
  },
  {
    id: 't3', title: 'Sous-tâches complètes', status: 'DOING', priority: 'MED', dueDate: null, assigneeIds: [], recurring: false, recurringSeriesId: null, createdBy: null,
    subtasks: [{ id: 's4', text: 'A', done: true }],
  },
]

describe('resolveTaskMove', () => {
  it('is a move when the task lands on a different column', () => {
    expect(resolveTaskMove(TASKS, 't1', 'DOING')).toEqual({ kind: 'move', task: TASKS[0] })
  })

  it('is nothing at all when the task lands back on its own column', () => {
    expect(resolveTaskMove(TASKS, 't1', 'TODO')).toEqual({ kind: 'nothing' })
  })

  it('is nothing at all when the task id is not among the tasks', () => {
    expect(resolveTaskMove(TASKS, 'does-not-exist', 'DOING')).toEqual({ kind: 'nothing' })
  })

  it('is a refusal, not nothing, when completing a task whose subtasks are still open', () => {
    // The distinction this type exists for: the server answers 409 here, so the caller has
    // something to say to the user — and said nothing for as long as both cases were null.
    expect(resolveTaskMove(TASKS, 't2', 'DONE')).toEqual({ kind: 'blocked', task: TASKS[1], openSubtasks: 2 })
  })

  it('counts only the subtasks that are still open', () => {
    const move = resolveTaskMove(TASKS, 't2', 'DONE')

    expect(move.kind === 'blocked' && move.openSubtasks).toBe(2)
  })

  it('is a move when completing a task whose subtasks are all done', () => {
    expect(resolveTaskMove(TASKS, 't3', 'DONE')).toEqual({ kind: 'move', task: TASKS[2] })
  })

  it('does not refuse a move away from DONE, whatever the subtasks say', () => {
    // Reopening a task is always allowed: the guard is about calling something finished.
    const doneWithOpenSubtask: Task = { ...TASKS[1], status: 'DONE' }

    expect(resolveTaskMove([doneWithOpenSubtask], 't2', 'TODO')).toEqual({ kind: 'move', task: doneWithOpenSubtask })
  })
})
