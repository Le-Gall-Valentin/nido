import { createContext, useContext } from 'react'
import type { ITasksApi } from './ITasksApi'
import type { IRecurringTaskSeriesApi } from './IRecurringTaskSeriesApi'

export type TasksApi = ITasksApi & IRecurringTaskSeriesApi

export const TasksApiContext = createContext<TasksApi | null>(null)

function useTasksApiContext(): TasksApi {
  const api = useContext(TasksApiContext)
  if (!api) {
    throw new Error('useTasksApi must be used within a TasksApiProvider')
  }
  return api
}

/** Narrows the injected api to task operations only — ISP: most hooks never touch series management. */
export function useTasksApi(): ITasksApi {
  return useTasksApiContext()
}

/** Narrows the injected api to recurring series management only. */
export function useRecurringTaskSeriesApi(): IRecurringTaskSeriesApi {
  return useTasksApiContext()
}
