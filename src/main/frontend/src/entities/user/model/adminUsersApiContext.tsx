import { createContext, useContext, type ReactNode } from 'react'
import type { IAdminUsersApi } from './IAdminUsersApi'

const AdminUsersApiContext = createContext<IAdminUsersApi | null>(null)

interface AdminUsersApiProviderProps {
  api: IAdminUsersApi
  children: ReactNode
}

/** Injects the IAdminUsersApi implementation consumed by the slice's hooks. */
export function AdminUsersApiProvider({ api, children }: AdminUsersApiProviderProps) {
  return <AdminUsersApiContext.Provider value={api}>{children}</AdminUsersApiContext.Provider>
}

export function useAdminUsersApi(): IAdminUsersApi {
  const api = useContext(AdminUsersApiContext)
  if (!api) {
    throw new Error('useAdminUsersApi must be used within an AdminUsersApiProvider')
  }
  return api
}
