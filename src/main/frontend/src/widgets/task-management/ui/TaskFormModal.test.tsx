import { render, screen, fireEvent, act } from '@testing-library/react'
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
  assigneeIds: ['u-1'], subtasks: [], recurring: false, recurringSeriesId: null, createdBy: null,
}

/** A local civil date N days back, so these cases stay true whenever the suite runs. */
function isoDaysAgo(days: number): string {
  const d = new Date()
  d.setDate(d.getDate() - days)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
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

  it('does not show the recurrence toggle when editing', () => {
    render(<TaskFormModal open onClose={vi.fn()} onSubmit={vi.fn()} initialTask={TASK} members={MEMBERS} isPersonal={false} />)

    expect(screen.queryByText('form.recurring_label')).toBeNull()
  })

  const WITH_SUBTASKS: Task = {
    ...TASK,
    subtasks: [{ id: 's-1', text: 'Acheter les sacs', done: true }, { id: 's-2', text: 'Trier le verre', done: false }],
  }
  const subtaskField = (index: number) => screen.getByLabelText(`form.subtask_label:{"index":${index}}`) as HTMLInputElement

  it('shows the subtasks of the task being edited, each in a field of its own', () => {
    render(<TaskFormModal open onClose={vi.fn()} onSubmit={vi.fn()} initialTask={WITH_SUBTASKS} members={MEMBERS} isPersonal={false} />)

    expect(subtaskField(1).value).toBe('Acheter les sacs')
    expect(subtaskField(2).value).toBe('Trier le verre')
  })

  it('editing sends back the subtask list, a kept subtask by its id and a new one without', () => {
    const onSubmit = vi.fn()
    render(<TaskFormModal open onClose={vi.fn()} onSubmit={onSubmit} initialTask={WITH_SUBTASKS} members={MEMBERS} isPersonal={false} />)

    fireEvent.change(subtaskField(1), { target: { value: 'Acheter les grands sacs' } })
    fireEvent.click(screen.getByLabelText('form.remove_subtask:{"index":2}'))
    fireEvent.change(screen.getByPlaceholderText('form.subtask_placeholder'), { target: { value: 'Sortir le bac' } })
    fireEvent.keyDown(screen.getByPlaceholderText('form.subtask_placeholder'), { key: 'Enter' })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({
      subtasks: [{ id: 's-1', text: 'Acheter les grands sacs' }, { text: 'Sortir le bac' }],
    }))
  })

  it('moves a subtask down the list from the keyboard, by its handle', async () => {
    // jsdom lays nothing out, so every row would sit at the same spot and the keyboard would have
    // nowhere to go: each row is given the place it takes on screen, one under the other.
    const layout = vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockImplementation(function (this: HTMLElement) {
      const row = this.closest('[data-subtask-row]')
      const top = row ? [...row.parentElement!.children].indexOf(row) * 40 : 0
      return { x: 0, y: top, top, left: 0, right: 300, bottom: top + 40, width: 300, height: 40, toJSON: () => ({}) }
    })
    // The keyboard sensor listens for the next key only once the current one has been handled.
    const settle = () => act(() => new Promise((resolve) => setTimeout(resolve, 0)))
    const onSubmit = vi.fn()
    render(<TaskFormModal open onClose={vi.fn()} onSubmit={onSubmit} initialTask={WITH_SUBTASKS} members={MEMBERS} isPersonal={false} />)

    const handle = screen.getByLabelText('form.move_subtask:{"index":1}')
    handle.focus()
    fireEvent.keyDown(handle, { code: 'Space' })
    await settle()
    fireEvent.keyDown(handle, { code: 'ArrowDown' })
    await settle()
    fireEvent.keyDown(handle, { code: 'Space' })
    await settle()
    fireEvent.click(screen.getByText('form.save'))
    layout.mockRestore()

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({
      subtasks: [{ id: 's-2', text: 'Trier le verre' }, { id: 's-1', text: 'Acheter les sacs' }],
    }))
  })

  it('refuses a subtask whose text was emptied out', () => {
    // The server refuses a blank subtask outright; saying so here keeps the rest of the edit.
    const onSubmit = vi.fn()
    render(<TaskFormModal open onClose={vi.fn()} onSubmit={onSubmit} initialTask={WITH_SUBTASKS} members={MEMBERS} isPersonal={false} />)

    fireEvent.change(subtaskField(1), { target: { value: '   ' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).not.toHaveBeenCalled()
    expect(screen.getByText('form.subtask_required')).toBeDefined()
  })

  it('creating a task sends the subtasks added to it, in order', () => {
    const onSubmit = vi.fn()
    render(<TaskFormModal open onClose={vi.fn()} onSubmit={onSubmit} initialTask={null} members={MEMBERS} isPersonal={false} />)

    fireEvent.change(screen.getByLabelText('form.title_label'), { target: { value: 'Poubelles' } })
    for (const text of ['Trier', 'Sortir']) {
      fireEvent.change(screen.getByPlaceholderText('form.subtask_placeholder'), { target: { value: text } })
      fireEvent.click(screen.getByLabelText('form.add_subtask'))
    }
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ subtasks: [{ text: 'Trier' }, { text: 'Sortir' }] }))
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
      recurrence: {
        intervalType: 'WEEKLY', intervalCount: 1, leadIntervalType: 'DAILY', leadIntervalCount: 0,
        anchorDate: '2026-01-07', endDate: null, rotationMemberIds: ['u-2', 'u-1'],
      },
    }))
  })

  it('creating a recurring task supports a yearly frequency', () => {
    const onSubmit = vi.fn()
    render(<TaskFormModal open onClose={vi.fn()} onSubmit={onSubmit} initialTask={null} members={MEMBERS} isPersonal={false} />)

    fireEvent.change(screen.getByLabelText('form.title_label'), { target: { value: 'Anniversaire' } })
    fireEvent.click(screen.getByText('form.recurring_label'))
    fireEvent.change(screen.getByLabelText('form.recurrence_interval_type_label'), { target: { value: 'YEARLY' } })
    fireEvent.change(screen.getByLabelText('form.recurrence_anchor_date_label'), { target: { value: '2026-01-07' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({
      recurrence: {
        intervalType: 'YEARLY', intervalCount: 1, leadIntervalType: 'DAILY', leadIntervalCount: 0,
        anchorDate: '2026-01-07', endDate: null, rotationMemberIds: [],
      },
    }))
  })

  it('rejects a lead time longer than the recurrence interval', () => {
    const onSubmit = vi.fn()
    render(<TaskFormModal open onClose={vi.fn()} onSubmit={onSubmit} initialTask={null} members={MEMBERS} isPersonal={false} />)

    fireEvent.change(screen.getByLabelText('form.title_label'), { target: { value: 'Sortir les poubelles' } })
    fireEvent.click(screen.getByText('form.recurring_label'))
    fireEvent.change(screen.getByLabelText('recurring_series.lead_time_type_label'), { target: { value: 'DAILY' } })
    fireEvent.change(screen.getByLabelText('recurring_series.lead_time_count_label'), { target: { value: '8' } })
    fireEvent.change(screen.getByLabelText('form.recurrence_anchor_date_label'), { target: { value: '2026-01-07' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).not.toHaveBeenCalled()
    expect(screen.getByText('form.lead_time_exceeds_interval')).toBeDefined()
  })

  it('creating a recurring task with an end date sends it through', () => {
    const onSubmit = vi.fn()
    render(<TaskFormModal open onClose={vi.fn()} onSubmit={onSubmit} initialTask={null} members={MEMBERS} isPersonal={false} />)

    fireEvent.change(screen.getByLabelText('form.title_label'), { target: { value: 'Sortir les poubelles' } })
    fireEvent.click(screen.getByText('form.recurring_label'))
    fireEvent.change(screen.getByLabelText('form.recurrence_anchor_date_label'), { target: { value: '2026-01-07' } })
    fireEvent.change(screen.getByLabelText('recurring_series.end_date_label'), { target: { value: '2027-01-01' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({
      recurrence: {
        intervalType: 'WEEKLY', intervalCount: 1, leadIntervalType: 'DAILY', leadIntervalCount: 0,
        anchorDate: '2026-01-07', endDate: '2027-01-01', rotationMemberIds: [],
      },
    }))
  })

  it('refuses a recurring task whose start date would backfill hundreds of occurrences', () => {
    // Weekly since 2000: the server refuses this, and before the mirror the form came back
    // with the generic "something went wrong" that invites a retry producing the same
    // refusal. A fixed date rather than a relative one — it is past the ceiling for good.
    const onSubmit = vi.fn()
    render(<TaskFormModal open onClose={vi.fn()} onSubmit={onSubmit} initialTask={null} members={MEMBERS} isPersonal={false} />)

    fireEvent.change(screen.getByLabelText('form.title_label'), { target: { value: 'Sortir les poubelles' } })
    fireEvent.click(screen.getByText('form.recurring_label'))
    fireEvent.change(screen.getByLabelText('form.recurrence_anchor_date_label'), { target: { value: '2000-01-01' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).not.toHaveBeenCalled()
    expect(screen.getByText((c) => c.startsWith('form.too_many_past_occurrences'))).toBeDefined()
  })

  it('still accepts a recurring task back-dated by a few weeks', () => {
    // The legitimate case the mirror must never catch.
    const onSubmit = vi.fn()
    render(<TaskFormModal open onClose={vi.fn()} onSubmit={onSubmit} initialTask={null} members={MEMBERS} isPersonal={false} />)

    fireEvent.change(screen.getByLabelText('form.title_label'), { target: { value: 'Sortir les poubelles' } })
    fireEvent.click(screen.getByText('form.recurring_label'))
    fireEvent.change(screen.getByLabelText('form.recurrence_anchor_date_label'), { target: { value: isoDaysAgo(21) } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).toHaveBeenCalled()
  })
})
