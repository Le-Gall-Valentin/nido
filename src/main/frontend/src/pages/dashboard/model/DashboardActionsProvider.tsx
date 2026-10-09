import type { ReactNode } from 'react'
import { DashboardActionsContext, type DashboardActions } from './dashboardActions'

export function DashboardActionsProvider({ value, children }: { value: DashboardActions; children: ReactNode }) {
  return <DashboardActionsContext.Provider value={value}>{children}</DashboardActionsContext.Provider>
}
