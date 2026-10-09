import { createContext, useContext } from 'react'
import type { IAdminUsersApi } from './IAdminUsersApi'

export const AdminUsersApiContext = createContext<IAdminUsersApi | null>(null)

export function useAdminUsersApi(): IAdminUsersApi {
  const api = useContext(AdminUsersApiContext)
  if (!api) {
    throw new Error('useAdminUsersApi must be used within an AdminUsersApiProvider')
  }
  return api
}
