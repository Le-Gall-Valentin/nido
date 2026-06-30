import type { ReactNode } from 'react'
import { authApi, AuthStoreProvider, type IAuthApi } from '@/features/auth'

interface Props {
  children: ReactNode
  api?: IAuthApi
}

export function AuthProvider({ children, api = authApi }: Props) {
  return <AuthStoreProvider api={api}>{children}</AuthStoreProvider>
}