import { useCallback, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import {
  calendarKey, toEventInput, useCalendarApi, useRecurringEventSeriesApi,
  type CalendarOccurrence, type ScheduleChange,
} from '@/entities/calendar'
import { tasksKey, useTasksApi } from '@/entities/tasks'
import { useKitchenApi } from '@/entities/kitchen'

/**
 * Moves an item of the calendar to another day or time, through whichever module owns it — an event,
 * one occurrence of a series, a task or a meal — showing the result before the server answers.
 * `canReschedule` says which items this can move.
 *
 * The cached windows are rewritten at once so the item stays where it was released, restored if the
 * write fails, and refetched either way — the same prefix every calendar mutation invalidates.
 */
export function useRescheduleOccurrence(spaceId: string) {
  const queryClient = useQueryClient()
  const calendarApi = useCalendarApi()
  const seriesApi = useRecurringEventSeriesApi()
  const tasksApi = useTasksApi()
  const kitchenApi = useKitchenApi()
  const [failed, setFailed] = useState(false)

  const persist = useCallback(async (o: CalendarOccurrence, change: ScheduleChange) => {
    if (o.source === 'EVENT') {
      const input = { ...toEventInput(o), ...change }
      if (o.materialized) {
        await calendarApi.updateEvent(spaceId, o.sourceId, input)
      } else if (o.seriesId && o.originalDate) {
        await seriesApi.detachOccurrence(spaceId, o.seriesId, o.originalDate, input)
      } else {
        throw new Error('An event occurrence with neither a row nor a slot cannot be moved')
      }
      return
    }
    if (o.source === 'TASK') {
      // The feed carries no priority; rewriting the task without it would reset it.
      const tasks = await queryClient.fetchQuery({ queryKey: tasksKey(spaceId), queryFn: () => tasksApi.listTasks(spaceId) })
      const task = tasks.find((candidate) => candidate.id === o.sourceId)
      if (!task) throw new Error('Task not found')
      await tasksApi.updateTask(spaceId, task.id, task.title, task.priority, change.startDate, task.assigneeIds)
      await queryClient.invalidateQueries({ queryKey: tasksKey(spaceId) })
      return
    }
    if (o.source === 'MEAL') {
      const entries = await kitchenApi.listMenuEntries(spaceId, o.startDate, o.startDate)
      const entry = entries.find((candidate) => candidate.id === o.sourceId)
      if (!entry) throw new Error('Menu entry not found')
      // Add first, remove second: the kitchen has no "move", and a failure between the two must
      // leave a visible duplicate rather than a meal that vanished.
      await kitchenApi.addMenuEntry(spaceId, change.startDate, entry.recipeId, entry.portions)
      await kitchenApi.removeMenuEntry(spaceId, entry.id)
      await queryClient.invalidateQueries({ queryKey: ['kitchen', spaceId, 'menu'] })
      return
    }
    throw new Error(`${o.source} is not draggable`)
  }, [calendarApi, seriesApi, tasksApi, kitchenApi, queryClient, spaceId])

  const reschedule = useCallback(async (o: CalendarOccurrence, change: ScheduleChange) => {
    const windows = { queryKey: [...calendarKey(spaceId), 'occurrences'] }
    // A refetch already on its way would answer with the item where it was, after the move is
    // shown. Cancelled first — and awaited, since a cancelled fetch restores its old state.
    await queryClient.cancelQueries(windows)
    const snapshot = queryClient.getQueriesData<CalendarOccurrence[]>(windows)
    queryClient.setQueriesData<CalendarOccurrence[]>(windows, (list) =>
      list?.map((candidate) => (candidate.sourceId === o.sourceId ? { ...candidate, ...change } : candidate)))
    try {
      await persist(o, change)
      setFailed(false)
    } catch {
      for (const [key, data] of snapshot) queryClient.setQueryData(key, data)
      setFailed(true)
    } finally {
      await queryClient.invalidateQueries({ queryKey: calendarKey(spaceId) })
    }
  }, [persist, queryClient, spaceId])

  const dismissFailure = useCallback(() => setFailed(false), [])

  return { reschedule, failed, dismissFailure }
}
