import type { ReactNode } from 'react'
import type { IDashboardApi } from './IDashboardApi'
import { DashboardApiContext } from './dashboardApiContext'

interface DashboardApiProviderProps {
  api: IDashboardApi
  children: ReactNode
}

/** Injects the IDashboardApi implementation consumed by useDashboard. */
export function DashboardApiProvider({ api, children }: DashboardApiProviderProps) {
  return <DashboardApiContext.Provider value={api}>{children}</DashboardApiContext.Provider>
}
