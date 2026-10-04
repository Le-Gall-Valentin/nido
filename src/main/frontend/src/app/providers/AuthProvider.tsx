import { useCallback, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { authApi, AuthStoreProvider, type IAuthApi } from '@/features/auth'

interface Props {
  children: ReactNode
  api?: IAuthApi
}

export function AuthProvider({ children, api = authApi }: Props) {
  const queryClient = useQueryClient()
  // Answers are cached by what was asked, never by who asked. Kept past a sign-out, they would be served to
  // the next account signing in on this tab: the previous one's spaces, and a space it cannot open.
  const forgetAnswers = useCallback(() => queryClient.clear(), [queryClient])
  return <AuthStoreProvider api={api} onSessionEnd={forgetAnswers}>{children}</AuthStoreProvider>
}
