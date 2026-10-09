import { createContext, useContext } from 'react'
import type { IDashboardApi } from './IDashboardApi'

export const DashboardApiContext = createContext<IDashboardApi | null>(null)

export function useDashboardApi(): IDashboardApi {
  const api = useContext(DashboardApiContext)
  if (!api) {
    throw new Error('useDashboardApi must be used within a DashboardApiProvider')
  }
  return api
}
