import { describe, it, expect, vi, beforeEach } from 'vitest'
import { client } from '@/shared/api'
import { tasksApi } from './tasksApi'
import type { Task } from '../model/types'

vi.mock('@/shared/api', () => ({ client: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), delete: vi.fn() } }))

const TASK: Task = { id: 't-1', title: 'Sortir les poubelles', status: 'TODO', priority: 'MED', dueDate: '2026-01-07', assigneeIds: [], subtasks: [], recurring: false, recurringSeriesId: null, createdBy: null }

describe('tasksApi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('lists tasks for a space', async () => {
    vi.mocked(client.get).mockResolvedValue({ data: [TASK] })
    const result = await tasksApi.listTasks('space-1')
    expect(client.get).toHaveBeenCalledWith('/spaces/space-1/tasks')
    expect(result).toEqual([TASK])
  })

  it('creates a one-off task', async () => {
    vi.mocked(client.post).mockResolvedValue({ data: TASK })
    const result = await tasksApi.createTask('space-1', 'Sortir les poubelles', 'MED', '2026-01-07', [], [])
    expect(client.post).toHaveBeenCalledWith('/spaces/space-1/tasks', {
      title: 'Sortir les poubelles', priority: 'MED', dueDate: '2026-01-07', assigneeIds: [], subtasks: [],
    })
    expect(result).toEqual(TASK)
  })

  it('creates a recurring task', async () => {
    vi.mocked(client.post).mockResolvedValue({ data: { ...TASK, recurring: true } })
    const recurrence = {
      intervalType: 'WEEKLY' as const, intervalCount: 1, leadIntervalType: 'DAILY' as const, leadIntervalCount: 0,
      anchorDate: '2026-01-07', endDate: null, rotationMemberIds: ['u-1'],
    }
    await tasksApi.createRecurringTask('space-1', 'Sortir les poubelles', 'MED', ['Vérifier le tri'], recurrence)
    expect(client.post).toHaveBeenCalledWith('/spaces/space-1/tasks', {
      title: 'Sortir les poubelles', priority: 'MED', subtasks: ['Vérifier le tri'], recurrence,
    })
  })

  it('updates a task', async () => {
    vi.mocked(client.patch).mockResolvedValue({ data: TASK })
    await tasksApi.updateTask('space-1', 't-1', 'Nouveau titre', 'HIGH', null, ['u-1'])
    expect(client.patch).toHaveBeenCalledWith('/spaces/space-1/tasks/t-1', {
      title: 'Nouveau titre', priority: 'HIGH', dueDate: null, assigneeIds: ['u-1'],
    })
  })

  it('changes task status', async () => {
    vi.mocked(client.post).mockResolvedValue({ data: { ...TASK, status: 'DOING' } })
    await tasksApi.changeTaskStatus('space-1', 't-1', 'DOING')
    expect(client.post).toHaveBeenCalledWith('/spaces/space-1/tasks/t-1/status', { status: 'DOING' })
  })

  it('toggles a subtask', async () => {
    vi.mocked(client.post).mockResolvedValue({ data: undefined })
    await tasksApi.toggleSubtask('space-1', 't-1', 'sub-1')
    expect(client.post).toHaveBeenCalledWith('/spaces/space-1/tasks/t-1/subtasks/sub-1/toggle')
  })

  it('deletes a task', async () => {
    vi.mocked(client.delete).mockResolvedValue({ data: undefined })
    await tasksApi.deleteTask('space-1', 't-1')
    expect(client.delete).toHaveBeenCalledWith('/spaces/space-1/tasks/t-1')
  })

  it('moves a task', async () => {
    vi.mocked(client.post).mockResolvedValue({ data: TASK })
    await tasksApi.moveTask('space-1', 't-1', 'space-2')
    expect(client.post).toHaveBeenCalledWith('/spaces/space-1/tasks/t-1/move', { destinationSpaceId: 'space-2' })
  })

  it('lists recurring task series', async () => {
    const series = [{ id: 's-1', title: 'Sortir les poubelles', priority: 'MED' as const, subtaskTemplates: [],
      intervalType: 'WEEKLY' as const, intervalCount: 1, leadIntervalType: 'DAILY' as const, leadIntervalCount: 0,
      anchorDate: '2026-01-07', endDate: null, rotationMemberIds: [] }]
    vi.mocked(client.get).mockResolvedValue({ data: series })
    const result = await tasksApi.listRecurringTaskSeries('space-1')
    expect(client.get).toHaveBeenCalledWith('/spaces/space-1/recurring-task-series')
    expect(result).toEqual(series)
  })

  it('updates a recurring task series', async () => {
    const recurrence = {
      intervalType: 'MONTHLY' as const, intervalCount: 1, leadIntervalType: 'WEEKLY' as const, leadIntervalCount: 1,
      anchorDate: '2026-01-07', endDate: null, rotationMemberIds: [],
    }
    vi.mocked(client.patch).mockResolvedValue({ data: { id: 's-1' } })
    await tasksApi.updateRecurringTaskSeries('space-1', 's-1', 'Sortir les poubelles', 'MED', [], recurrence)
    expect(client.patch).toHaveBeenCalledWith('/spaces/space-1/recurring-task-series/s-1',
      { title: 'Sortir les poubelles', priority: 'MED', subtasks: [], recurrence })
  })

  it('deletes a recurring task series', async () => {
    vi.mocked(client.delete).mockResolvedValue({ data: undefined })
    await tasksApi.deleteRecurringTaskSeries('space-1', 's-1')
    expect(client.delete).toHaveBeenCalledWith('/spaces/space-1/recurring-task-series/s-1')
  })
})
