import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import type { CalendarOccurrence } from '@/entities/calendar'
import { OccurrenceRouter } from './OccurrenceRouter'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}))

// What each module would answer, set per test. The editors they own are stubbed: this file only
// decides which one opens, and on what.
const state = vi.hoisted(() => ({
  tasks: { data: undefined as unknown[] | undefined, isPending: true },
  taskSeries: [] as unknown[],
  financeSeries: { data: undefined as unknown[] | undefined, isPending: true },
  categories: { data: undefined as unknown[] | undefined, isPending: true },
  goals: { data: undefined as unknown[] | undefined, isPending: true },
  updateGoal: { mutate: (() => {}) as (input: unknown, options?: { onSuccess?: () => void }) => void, isError: false },
}))

vi.mock('@/entities/tasks', () => ({
  useTasks: () => state.tasks,
  useRecurringTaskSeries: () => ({ data: state.taskSeries }),
}))
vi.mock('@/entities/finance', () => ({
  useRecurringSeries: () => state.financeSeries,
  useCategories: () => state.categories,
  useSavingsGoals: () => state.goals,
  useUpdateSavingsGoal: () => state.updateGoal,
}))
vi.mock('@/widgets/task-management', () => ({
  TaskDetailModal: ({ task, series }: { task: { title: string }; series: { id: string } | null }) =>
    <p>task {task.title} of {series?.id ?? 'no series'}</p>,
}))
vi.mock('@/widgets/finance-recurring-series', () => ({
  FinanceRecurringSeriesPanel: ({ series }: { series: unknown[] }) => <p>finance panel with {series.length} series</p>,
}))
vi.mock('@/widgets/savings-goal', () => ({
  SavingsGoalFormModal: ({ goal, onSubmit, submitError }: {
    goal: { name: string }; onSubmit: (input: object) => void; submitError?: string | null
  }) => (
    <>
      <button type="button" onClick={() => onSubmit({ targetDate: '2026-10-22' })}>save {goal.name}</button>
      {submitError && <p>{submitError}</p>}
    </>
  ),
}))

const base: CalendarOccurrence = {
  source: 'TASK', sourceId: 't1', seriesId: null, originalDate: null, materialized: true,
  title: 'Poubelles', description: null, location: null, allDay: true,
  startDate: '2026-10-05', startTime: null, endDate: '2026-10-05', endTime: null, color: null, participantIds: [],
}

function route(occurrence: CalendarOccurrence) {
  const onOpenInModule = vi.fn()
  const onClose = vi.fn()
  const { container } = render(
    <OccurrenceRouter spaceId="space-1" occurrence={occurrence} members={[]} onOpenInModule={onOpenInModule} onClose={onClose} />)
  return { onOpenInModule, onClose, container }
}

describe('OccurrenceRouter', () => {
  beforeEach(() => {
    state.tasks = { data: undefined, isPending: true }
    state.taskSeries = []
    state.financeSeries = { data: undefined, isPending: true }
    state.categories = { data: undefined, isPending: true }
    state.goals = { data: undefined, isPending: true }
    state.updateGoal = { mutate: vi.fn(), isError: false }
  })

  describe('a task', () => {
    it('opens the task with the series it belongs to', () => {
      state.tasks = { data: [{ id: 't1', title: 'Poubelles', recurringSeriesId: 'ts1' }], isPending: false }
      state.taskSeries = [{ id: 'ts0' }, { id: 'ts1' }]
      route(base)
      expect(screen.getByText('task Poubelles of ts1')).toBeTruthy()
    })

    it('opens a one-off task on its own', () => {
      state.tasks = { data: [{ id: 't1', title: 'Poubelles', recurringSeriesId: null }], isPending: false }
      route(base)
      expect(screen.getByText('task Poubelles of no series')).toBeTruthy()
    })

    it('waits for the tasks before opening one', () => {
      route(base)
      expect(screen.getByRole('status')).toBeTruthy()
    })

    it('says so when the task is gone', () => {
      state.tasks = { data: [], isPending: false }
      route(base)
      expect(screen.getByText('detail.missing')).toBeTruthy()
    })

    it('sends an occurrence still to come to the tasks page, having no task to open', () => {
      const projected = { ...base, sourceId: 'ts1:2026-10-12', materialized: false }
      const { onOpenInModule } = route(projected)
      expect(screen.getByText('detail.projected_hint')).toBeTruthy()
      fireEvent.click(screen.getByRole('button', { name: 'detail.open_in.TASK' }))
      expect(onOpenInModule).toHaveBeenCalledWith(projected)
    })
  })

  describe('money', () => {
    it('opens the recurring money of the space once it is read', () => {
      state.financeSeries = { data: [{}, {}], isPending: false }
      state.categories = { data: [], isPending: false }
      route({ ...base, source: 'FINANCE' })
      expect(screen.getByText('finance panel with 2 series')).toBeTruthy()
    })

    it('waits for the categories too', () => {
      state.financeSeries = { data: [], isPending: false }
      route({ ...base, source: 'FINANCE' })
      expect(screen.getByRole('status')).toBeTruthy()
    })
  })

  describe('a savings deadline', () => {
    const deadline = { ...base, source: 'SAVINGS' as const, sourceId: 'g1', title: 'Vacances' }

    it('opens the goal and moves it from here, closing once saved', () => {
      state.goals = { data: [{ id: 'g1', name: 'Vacances' }], isPending: false }
      state.updateGoal = { mutate: vi.fn((_input: unknown, options?: { onSuccess?: () => void }) => options?.onSuccess?.()), isError: false }
      const { onClose } = route(deadline)
      fireEvent.click(screen.getByRole('button', { name: 'save Vacances' }))
      expect(state.updateGoal.mutate).toHaveBeenCalledWith({ goalId: 'g1', targetDate: '2026-10-22' }, expect.anything())
      expect(onClose).toHaveBeenCalled()
    })

    it('says so and stays open when the goal could not be saved', () => {
      state.goals = { data: [{ id: 'g1', name: 'Vacances' }], isPending: false }
      state.updateGoal = { mutate: vi.fn(), isError: true }
      const { onClose } = route(deadline)
      expect(screen.getByText('form.save_failed')).toBeTruthy()
      expect(onClose).not.toHaveBeenCalled()
    })

    it('waits for the goals', () => {
      route(deadline)
      expect(screen.getByRole('status')).toBeTruthy()
    })

    it('says so when the goal is gone', () => {
      state.goals = { data: [], isPending: false }
      route(deadline)
      expect(screen.getByText('detail.missing')).toBeTruthy()
    })
  })

  describe('a meal', () => {
    const dinner = { ...base, source: 'MEAL' as const, sourceId: 'm1', title: 'Gratin' }

    it('shows it and opens the menu it belongs to', () => {
      const { onOpenInModule } = route(dinner)
      expect(screen.getByText('Gratin')).toBeTruthy()
      expect(screen.getByText('2026-10-05 · source.MEAL')).toBeTruthy()
      expect(screen.queryByText('detail.projected_hint')).toBeNull()
      fireEvent.click(screen.getByRole('button', { name: 'detail.open_in.MEAL' }))
      expect(onOpenInModule).toHaveBeenCalledWith(dinner)
    })

    it('says when it is still to come', () => {
      route({ ...dinner, materialized: false })
      expect(screen.getByText('detail.projected_hint')).toBeTruthy()
    })
  })

  it('leaves an event to the page, which owns its form', () => {
    const { container } = route({ ...base, source: 'EVENT' })
    expect(container.innerHTML).toBe('')
  })
})
