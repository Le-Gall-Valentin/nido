import { describe, it, expect } from 'vitest'
import { resolveTaskMove } from './resolveTaskMove'
import type { Task } from '@/entities/tasks'

const TASKS: Task[] = [
  { id: 't1', title: 'À faire', status: 'TODO', priority: 'MED', dueDate: null, assigneeIds: [], subtasks: [], recurring: false },
  {
    id: 't2', title: 'Sous-tâches ouvertes', status: 'DOING', priority: 'MED', dueDate: null, assigneeIds: [], recurring: false,
    subtasks: [{ id: 's1', text: 'A', done: true }, { id: 's2', text: 'B', done: false }],
  },
  {
    id: 't3', title: 'Sous-tâches complètes', status: 'DOING', priority: 'MED', dueDate: null, assigneeIds: [], recurring: false,
    subtasks: [{ id: 's3', text: 'A', done: true }],
  },
]

describe('resolveTaskMove', () => {
  it('returns the task when dropped on a different column', () => {
    expect(resolveTaskMove(TASKS, 't1', 'DOING')).toEqual(TASKS[0])
  })

  it('returns null when dropped back on its own current column', () => {
    expect(resolveTaskMove(TASKS, 't1', 'TODO')).toBeNull()
  })

  it('returns null when the task id is not found', () => {
    expect(resolveTaskMove(TASKS, 'does-not-exist', 'DOING')).toBeNull()
  })

  it('returns null when moving to DONE with an open subtask', () => {
    expect(resolveTaskMove(TASKS, 't2', 'DONE')).toBeNull()
  })

  it('returns the task when moving to DONE with every subtask done', () => {
    expect(resolveTaskMove(TASKS, 't3', 'DONE')).toEqual(TASKS[2])
  })
})
