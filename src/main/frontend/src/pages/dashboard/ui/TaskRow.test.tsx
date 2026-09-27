import { fireEvent, screen, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import type { TaskItem } from '@/entities/dashboard'
import { TaskRow } from './TaskRow'
import { CardList } from './CardRow'
import { renderWithActions } from '../test/renderWithActions'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const TASK: TaskItem = {
  id: 't-1', title: 'Sortir les poubelles', dueDate: '2026-09-26', priority: 'MED', status: 'TODO',
  assigneeIds: ['u-me'], subtasksDone: 0, subtasksTotal: 0, recurring: true, mine: true,
}

function renderRow(task: TaskItem, options: Parameters<typeof renderWithActions>[1] = {}) {
  return renderWithActions(<CardList><TaskRow task={task} dateStyle="overdue" /></CardList>, options)
}

describe('TaskRow', () => {
  it('ticks a task done', async () => {
    const changeTaskStatus = vi.fn().mockResolvedValue(undefined)
    renderRow(TASK, { tasksApi: { changeTaskStatus } })

    fireEvent.click(screen.getByRole('checkbox', { name: 'tasks.mark_done:{"title":"Sortir les poubelles"}' }))

    await waitFor(() => expect(changeTaskStatus).toHaveBeenCalledWith('space-1', 't-1', 'DONE'))
  })

  it('will not tick a task whose subtasks are still open, and says why', () => {
    const changeTaskStatus = vi.fn()
    renderRow({ ...TASK, subtasksDone: 1, subtasksTotal: 3 }, { tasksApi: { changeTaskStatus } })
    const box = screen.getByRole('checkbox')

    fireEvent.click(box)

    expect(box.getAttribute('aria-disabled')).toBe('true')
    expect(box.getAttribute('title')).toBe('tasks.subtasks_left:{"count":2}')
    expect(changeTaskStatus).not.toHaveBeenCalled()
    expect(screen.getByText('1/3')).toBeDefined()
  })

  it('says why a blocked task cannot be ticked when tapped, where no tooltip ever shows', () => {
    renderRow({ ...TASK, subtasksDone: 1, subtasksTotal: 3 })
    expect(screen.queryByRole('status')).toBeNull()

    fireEvent.click(screen.getByRole('checkbox'))

    expect(screen.getByRole('status').textContent).toBe('tasks.subtasks_left:{"count":2}')
  })

  it('tells me when a rotating chore is my turn', () => {
    renderRow(TASK)
    expect(screen.getByText('tasks.your_turn')).toBeDefined()
  })

  it('states an overdue date in red and shows the priority', () => {
    renderRow({ ...TASK, dueDate: '2026-09-22', recurring: false })
    expect(screen.getByText('tasks.due_on:{"date":"22 sept."}').className).toContain('text-status-red')
    expect(screen.getByText('priority.MED')).toBeDefined()
  })

  it('offers no checkbox to a viewer', () => {
    renderRow(TASK, { actions: { canWrite: false } })
    expect(screen.queryByRole('checkbox')).toBeNull()
  })
})
