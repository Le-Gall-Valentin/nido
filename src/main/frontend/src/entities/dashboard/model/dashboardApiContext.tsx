import { createContext, useContext, type ReactNode } from 'react'
import type { IDashboardApi } from './IDashboardApi'

const DashboardApiContext = createContext<IDashboardApi | null>(null)

interface DashboardApiProviderProps {
  api: IDashboardApi
  children: ReactNode
}

/** Injects the IDashboardApi implementation consumed by useDashboard. */
export function DashboardApiProvider({ api, children }: DashboardApiProviderProps) {
  return <DashboardApiContext.Provider value={api}>{children}</DashboardApiContext.Provider>
}

export function useDashboardApi(): IDashboardApi {
  const api = useContext(DashboardApiContext)
  if (!api) {
    throw new Error('useDashboardApi must be used within a DashboardApiProvider')
  }
  return api
}
