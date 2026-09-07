import { render, screen, within } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import type { SpaceMember } from '@/entities/space'
import type { RecurringTaskSeries, Task } from '@/entities/tasks'
import { TaskDetailModal } from './TaskDetailModal'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const MEMBERS: SpaceMember[] = [
  { userId: 'u-1', username: 'alice', email: 'a@test.com', role: 'MEMBER', joinedAt: '2024-01-01T00:00:00Z' },
  { userId: 'u-2', username: 'bob', email: 'b@test.com', role: 'MEMBER', joinedAt: '2024-01-01T00:00:00Z' },
]

const TASK: Task = {
  id: 't-1', title: 'Sortir les poubelles', status: 'TODO', priority: 'MED', dueDate: '2026-01-07',
  assigneeIds: ['u-1'], subtasks: [{ id: 's-1', text: 'Vérifier le tri', done: false }],
  recurring: false, recurringSeriesId: null, createdBy: 'u-2',
}

const SERIES: RecurringTaskSeries = {
  id: 'series-1', title: 'Sortir les poubelles', priority: 'MED', subtaskTemplates: [],
  intervalType: 'WEEKLY', intervalCount: 1, leadIntervalType: 'DAILY', leadIntervalCount: 0,
  anchorDate: '2026-01-07', endDate: null, rotationMemberIds: ['u-1', 'u-2'], createdBy: 'u-2',
}

describe('TaskDetailModal', () => {
  it('shows the title, status, due date, creator and assignees', () => {
    render(<TaskDetailModal task={TASK} series={null} members={MEMBERS} onClose={vi.fn()} />)

    expect(screen.getByText('Sortir les poubelles')).toBeDefined()
    expect(screen.getByText('column.TODO')).toBeDefined()
    expect(screen.getByText('2026-01-07')).toBeDefined()
    expect(screen.getByText('bob')).toBeDefined()
    expect(screen.getByText('alice')).toBeDefined()
  })

  it('shows a fallback when there is no due date', () => {
    render(<TaskDetailModal task={{ ...TASK, dueDate: null }} series={null} members={MEMBERS} onClose={vi.fn()} />)

    expect(screen.getByText('detail.no_due_date')).toBeDefined()
  })

  it('shows a fallback creator label when createdBy is null', () => {
    render(<TaskDetailModal task={{ ...TASK, createdBy: null }} series={null} members={MEMBERS} onClose={vi.fn()} />)

    expect(screen.getByText('detail.unknown_creator')).toBeDefined()
  })

  it('shows a fallback when nobody is assigned', () => {
    render(<TaskDetailModal task={{ ...TASK, assigneeIds: [] }} series={null} members={MEMBERS} onClose={vi.fn()} />)

    expect(screen.getByText('detail.no_assignee')).toBeDefined()
  })

  it('shows every subtask with its done state', () => {
    render(<TaskDetailModal task={TASK} series={null} members={MEMBERS} onClose={vi.fn()} />)

    expect(screen.getByText('Vérifier le tri')).toBeDefined()
  })

  it('shows every rotation participant and marks whose turn it is for a recurring occurrence', () => {
    render(<TaskDetailModal task={TASK} series={SERIES} members={MEMBERS} onClose={vi.fn()} />)

    const section = screen.getByText('detail.rotation_participants_label').closest('div')!
    const aliceRow = within(section).getByText('alice').closest('li')!
    const bobRow = within(section).getByText('bob').closest('li')!
    expect(aliceRow.textContent).toContain('detail.this_occurrence_label')
    expect(bobRow.textContent).not.toContain('detail.this_occurrence_label')
  })

  it('does not show a rotation section for a one-off task', () => {
    render(<TaskDetailModal task={TASK} series={null} members={MEMBERS} onClose={vi.fn()} />)

    expect(screen.queryByText('detail.rotation_participants_label')).toBeNull()
  })
})
