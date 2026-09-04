import { createContext, useContext, type ReactNode } from 'react'
import type { ITasksApi } from './ITasksApi'

const TasksApiContext = createContext<ITasksApi | null>(null)

interface TasksApiProviderProps {
  api: ITasksApi
  children: ReactNode
}

/** Injects the ITasksApi implementation consumed by the tasks page's hooks. */
export function TasksApiProvider({ api, children }: TasksApiProviderProps) {
  return <TasksApiContext.Provider value={api}>{children}</TasksApiContext.Provider>
}

export function useTasksApi(): ITasksApi {
  const api = useContext(TasksApiContext)
  if (!api) {
    throw new Error('useTasksApi must be used within a TasksApiProvider')
  }
  return api
}
