import { describe, it, expect, vi } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import { CalendarApiProvider, occurrencesKey, type CalendarApi, type CalendarOccurrence } from '@/entities/calendar'
import { TasksApiProvider, type TasksApi } from '@/entities/tasks'
import { KitchenApiProvider, type IKitchenApi } from '@/entities/kitchen'
import { useApplyDrop } from './useApplyDrop'

const change = { allDay: false, startDate: '2026-09-25', startTime: '09:00', endDate: '2026-09-25', endTime: '10:00' }

function occurrence(overrides: Partial<CalendarOccurrence>): CalendarOccurrence {
  return {
    source: 'EVENT', sourceId: 'e1', seriesId: null, originalDate: null, materialized: true,
    title: 'Concert', description: 'Billets', location: 'Salle', allDay: false,
    startDate: '2026-09-23', startTime: '20:00', endDate: '2026-09-23', endTime: '21:00',
    color: null, participantIds: [], ...overrides,
  }
}

function setup(apis: { calendar?: Partial<CalendarApi>; tasks?: Partial<TasksApi>; kitchen?: Partial<IKitchenApi> } = {}) {
  const queryClient = createTestQueryClient()
  const calendar = { updateEvent: vi.fn().mockResolvedValue({}), detachOccurrence: vi.fn().mockResolvedValue({}), listOccurrences: vi.fn().mockResolvedValue([]), ...apis.calendar } as unknown as CalendarApi
  const tasks = { listTasks: vi.fn().mockResolvedValue([]), updateTask: vi.fn().mockResolvedValue({}), ...apis.tasks } as unknown as TasksApi
  const kitchen = { listMenuEntries: vi.fn().mockResolvedValue([]), addMenuEntry: vi.fn().mockResolvedValue({}), removeMenuEntry: vi.fn().mockResolvedValue(undefined), ...apis.kitchen } as unknown as IKitchenApi
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <CalendarApiProvider api={calendar}>
        <TasksApiProvider api={tasks}>
          <KitchenApiProvider api={kitchen}>{children}</KitchenApiProvider>
        </TasksApiProvider>
      </CalendarApiProvider>
    </QueryClientProvider>)
  const hook = renderHook(() => useApplyDrop('space-1'), { wrapper })
  return { hook, calendar, tasks, kitchen, queryClient }
}

describe('useApplyDrop', () => {
  it('patches a one-off event and keeps its description and location', async () => {
    const { hook, calendar } = setup()
    await act(() => hook.result.current.apply(occurrence({}), change))
    expect(calendar.updateEvent).toHaveBeenCalledWith('space-1', 'e1', expect.objectContaining({
      startDate: '2026-09-25', startTime: '09:00', description: 'Billets', location: 'Salle',
    }))
  })

  it('moves only one occurrence of a series, through its original slot', async () => {
    const { hook, calendar } = setup()
    await act(() => hook.result.current.apply(
      occurrence({ materialized: false, sourceId: 's-1:2026-09-23', seriesId: 's-1', originalDate: '2026-09-23' }), change))
    expect(calendar.detachOccurrence).toHaveBeenCalledWith('space-1', 's-1', '2026-09-23', expect.objectContaining({ startDate: '2026-09-25' }))
    expect(calendar.updateEvent).not.toHaveBeenCalled()
  })

  it('changes a task\'s due date, keeping the priority the feed does not carry', async () => {
    const { hook, tasks } = setup({ tasks: { listTasks: vi.fn().mockResolvedValue([
      { id: 't1', title: 'Poubelles', status: 'TODO', priority: 'HIGH', dueDate: '2026-09-23', assigneeIds: ['u1'],
        subtasks: [], recurring: false, recurringSeriesId: null, createdBy: null },
    ]) } })
    await act(() => hook.result.current.apply(occurrence({ source: 'TASK', sourceId: 't1', allDay: true, startTime: null, endTime: null }),
      { ...change, allDay: true, startTime: null, endTime: null }))
    expect(tasks.updateTask).toHaveBeenCalledWith('space-1', 't1', 'Poubelles', 'HIGH', '2026-09-25', ['u1'])
  })

  it('moves a meal by adding it on the new day before removing it from the old one', async () => {
    const calls: string[] = []
    const { hook, kitchen } = setup({ kitchen: {
      listMenuEntries: vi.fn().mockResolvedValue([{ id: 'm1', date: '2026-09-23', recipeId: 'r1', recipeName: 'Gratin', recipeCategory: 'PLAT', portions: 4, position: 0 }]),
      addMenuEntry: vi.fn().mockImplementation(async () => { calls.push('add') }),
      removeMenuEntry: vi.fn().mockImplementation(async () => { calls.push('remove') }),
    } })
    await act(() => hook.result.current.apply(occurrence({ source: 'MEAL', sourceId: 'm1', allDay: true, startTime: null, endTime: null }),
      { ...change, allDay: true, startTime: null, endTime: null }))
    expect(kitchen.addMenuEntry).toHaveBeenCalledWith('space-1', '2026-09-25', 'r1', 4)
    expect(calls).toEqual(['add', 'remove'])
  })

  it('keeps the meal where it was when adding it elsewhere fails — a failure never loses it', async () => {
    const { hook, kitchen } = setup({ kitchen: {
      listMenuEntries: vi.fn().mockResolvedValue([{ id: 'm1', date: '2026-09-23', recipeId: 'r1', recipeName: 'Gratin', recipeCategory: 'PLAT', portions: 4, position: 0 }]),
      addMenuEntry: vi.fn().mockRejectedValue(new Error('nope')),
    } })
    await act(() => hook.result.current.apply(occurrence({ source: 'MEAL', sourceId: 'm1', allDay: true, startTime: null, endTime: null }),
      { ...change, allDay: true, startTime: null, endTime: null }))
    expect(kitchen.removeMenuEntry).not.toHaveBeenCalled()
    expect(hook.result.current.failed).toBe(true)
  })

  it('shows the move at once, and puts it back when the write fails', async () => {
    let reject: (reason?: unknown) => void = () => {}
    const { hook, queryClient } = setup({ calendar: { updateEvent: vi.fn().mockImplementation(() => new Promise((_, r) => { reject = r })) } })
    const key = occurrencesKey('space-1', '2026-09-21', '2026-09-27')
    // On the page this window has a live observer; here nothing observes it, and the test client's
    // gcTime of 0 would collect it between two ticks.
    queryClient.setQueryDefaults(key, { gcTime: Infinity })
    queryClient.setQueryData(key, [occurrence({})])

    let pending: Promise<void> = Promise.resolve()
    act(() => { pending = hook.result.current.apply(occurrence({}), change) })
    // Before the server has answered: the write is still pending.
    await waitFor(() => expect(queryClient.getQueryData<CalendarOccurrence[]>(key)?.[0].startDate).toBe('2026-09-25'))

    await act(async () => { reject(new Error('refused')); await pending })
    expect(queryClient.getQueryData<CalendarOccurrence[]>(key)?.[0].startDate).toBe('2026-09-23')
    expect(hook.result.current.failed).toBe(true)
  })

  it('keeps the move on screen when a refetch of that window was already on its way', async () => {
    // Paging away and back, or a second drop right after a first, starts a refetch; its stale
    // answer used to land after the optimistic move and put the item back where it was.
    const { hook, queryClient } = setup({ calendar: { updateEvent: vi.fn().mockReturnValue(new Promise(() => {})) } })
    const key = occurrencesKey('space-1', '2026-09-21', '2026-09-27')
    queryClient.setQueryDefaults(key, { gcTime: Infinity })
    queryClient.setQueryData(key, [occurrence({})])
    let answer: (data: CalendarOccurrence[]) => void = () => {}
    const inFlight = queryClient.fetchQuery({ queryKey: key, queryFn: () => new Promise<CalendarOccurrence[]>((r) => { answer = r }) })
    inFlight.catch(() => {})

    act(() => { void hook.result.current.apply(occurrence({}), change) })
    await waitFor(() => expect(queryClient.getQueryData<CalendarOccurrence[]>(key)?.[0].startDate).toBe('2026-09-25'))
    await act(async () => { answer([occurrence({})]); await Promise.resolve() })

    expect(queryClient.getQueryData<CalendarOccurrence[]>(key)?.[0].startDate).toBe('2026-09-25')
  })
})
