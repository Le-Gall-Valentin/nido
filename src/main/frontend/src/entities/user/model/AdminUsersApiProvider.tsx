import type { ReactNode } from 'react'
import type { IAdminUsersApi } from './IAdminUsersApi'
import { AdminUsersApiContext } from './adminUsersApiContext'

interface AdminUsersApiProviderProps {
  api: IAdminUsersApi
  children: ReactNode
}

/** Injects the IAdminUsersApi implementation consumed by the slice's hooks. */
export function AdminUsersApiProvider({ api, children }: AdminUsersApiProviderProps) {
  return <AdminUsersApiContext.Provider value={api}>{children}</AdminUsersApiContext.Provider>
}
