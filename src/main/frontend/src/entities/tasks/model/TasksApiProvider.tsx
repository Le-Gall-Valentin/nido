import type { ReactNode } from 'react'
import { TasksApiContext, type TasksApi } from './tasksApiContext'

interface TasksApiProviderProps {
  api: TasksApi
  children: ReactNode
}

/** Injects the ITasksApi/IRecurringTaskSeriesApi implementation consumed by the tasks page's hooks. */
export function TasksApiProvider({ api, children }: TasksApiProviderProps) {
  return <TasksApiContext.Provider value={api}>{children}</TasksApiContext.Provider>
}
