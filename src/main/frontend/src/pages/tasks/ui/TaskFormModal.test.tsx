import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import type { SpaceMember } from '@/entities/space'
import type { Task } from '@/entities/tasks'
import { TaskFormModal } from './TaskFormModal'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const MEMBERS: SpaceMember[] = [
  { userId: 'u-1', username: 'alice', email: 'a@test.com', role: 'MEMBER', joinedAt: '2024-01-01T00:00:00Z' },
  { userId: 'u-2', username: 'bob', email: 'b@test.com', role: 'MEMBER', joinedAt: '2024-01-01T00:00:00Z' },
]

const TASK: Task = {
  id: 't-1', title: 'Sortir les poubelles', status: 'TODO', priority: 'MED', dueDate: '2026-01-07',
  assigneeIds: ['u-1'], subtasks: [], recurring: false,
}

describe('TaskFormModal', () => {
  it('rejects an empty title', () => {
    const onSubmit = vi.fn()
    render(<TaskFormModal open onClose={vi.fn()} onSubmit={onSubmit} initialTask={null} members={MEMBERS} isPersonal={false} />)

    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).not.toHaveBeenCalled()
    expect(screen.getByText('form.title_required')).toBeDefined()
  })

  it('creates a one-off task with the entered fields', () => {
    const onSubmit = vi.fn()
    render(<TaskFormModal open onClose={vi.fn()} onSubmit={onSubmit} initialTask={null} members={MEMBERS} isPersonal={false} />)

    fireEvent.change(screen.getByLabelText('form.title_label'), { target: { value: 'Nouvelle tâche' } })
    fireEvent.click(screen.getByText('alice'))
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({
      title: 'Nouvelle tâche', priority: 'MED', assigneeIds: ['u-1'], recurrence: null,
    }))
  })

  it('hides the assignee picker in a personal space', () => {
    render(<TaskFormModal open onClose={vi.fn()} onSubmit={vi.fn()} initialTask={null} members={[]} isPersonal />)

    expect(screen.queryByText('form.assignees_label')).toBeNull()
  })

  it('does not show the recurrence toggle or the subtasks editor when editing', () => {
    render(<TaskFormModal open onClose={vi.fn()} onSubmit={vi.fn()} initialTask={TASK} members={MEMBERS} isPersonal={false} />)

    expect(screen.queryByText('form.recurring_label')).toBeNull()
    expect(screen.queryByText('form.subtasks_title')).toBeNull()
  })

  it('submits an update with the edited fields when editing', () => {
    const onSubmit = vi.fn()
    render(<TaskFormModal open onClose={vi.fn()} onSubmit={onSubmit} initialTask={TASK} members={MEMBERS} isPersonal={false} />)

    fireEvent.change(screen.getByLabelText('form.title_label'), { target: { value: 'Titre modifié' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ title: 'Titre modifié', assigneeIds: ['u-1'], recurrence: null }))
  })

  it('creating a recurring task sends the recurrence block with the selected rotation order', () => {
    const onSubmit = vi.fn()
    render(<TaskFormModal open onClose={vi.fn()} onSubmit={onSubmit} initialTask={null} members={MEMBERS} isPersonal={false} />)

    fireEvent.change(screen.getByLabelText('form.title_label'), { target: { value: 'Sortir les poubelles' } })
    fireEvent.click(screen.getByText('form.recurring_label'))
    fireEvent.click(screen.getByText('bob'))
    fireEvent.click(screen.getByText('alice'))
    fireEvent.change(screen.getByLabelText('form.recurrence_anchor_date_label'), { target: { value: '2026-01-07' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({
      title: 'Sortir les poubelles', assigneeIds: [], subtasks: [],
      recurrence: { intervalType: 'WEEKLY', intervalCount: 1, anchorDate: '2026-01-07', rotationMemberIds: ['u-2', 'u-1'] },
    }))
  })
})
